package com.zhuinden.monarchy;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.realm.OrderedCollectionChangeSet;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Monarchy is a wrapper around Realm that simplifies its lifecycle management, and leverages the power of LiveData.
 *
 * Not only does it auto-manage the Realm lifecycle, but specifically prevents the ability to use Realm in such a way that it is impossible to leave a Realm instance open by accident.
 *
 * In case of `copied` or `mapped` results, the queries are evaluated on a background looper thread.
 *
 * In case of `managed` results, the RealmResults is provided along with its change set.
 */
public final class Monarchy {
    /**
     * A class that contains the RealmResults and the OrderedCollectionChangeSet.
     *
     * @param <T> the RealmModel type
     */
    public static class ManagedChangeSet<T extends RealmModel> {
        private final RealmResults<T> realmResults;
        private final OrderedCollectionChangeSet orderedCollectionChangeSet;

        ManagedChangeSet(RealmResults<T> realmResults, OrderedCollectionChangeSet orderedCollectionChangeSet) {
            this.realmResults = realmResults;
            this.orderedCollectionChangeSet = orderedCollectionChangeSet;
        }

        /**
         * Gets the RealmResults.
         *
         * @return the RealmResults
         */
        public RealmResults<T> getRealmResults() {
            return realmResults;
        }

        /**
         * Gets the ordered collection change set.
         *
         * @return the change set
         */
        @Nullable
        public OrderedCollectionChangeSet getOrderedCollectionChangeSet() {
            return orderedCollectionChangeSet;
        }
    }

    private final Executor writeScheduler = Executors.newSingleThreadExecutor();

    private static volatile RealmConfiguration invalidDefaultConfig;

    /**
     * Initializes Realm as usual, except sets a default configuration to detect if a custom default is properly set.
     *
     * @param context app context
     */
    public static void init(Context context) {
        Realm.init(context);
        invalidDefaultConfig = new RealmConfiguration.Builder().build();
        Realm.setDefaultConfiguration(invalidDefaultConfig);
    }

    /**
     * Calls Realm.setDefaultConfiguration(config).
     *
     * @param realmConfiguration realm configuration
     */
    public static void setDefaultConfiguration(RealmConfiguration realmConfiguration) {
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    /**
     * Returns the default configuration.
     *
     * @return the custom default configuration
     * @throws IllegalStateException if the invalid default configuration is still set.
     */
    public static RealmConfiguration getDefaultConfiguration() {
        final RealmConfiguration config = Realm.getDefaultConfiguration();
        if(config == invalidDefaultConfig) {
            throw new IllegalStateException("No default configuration is set!");
        }
        return config;
    }

    private volatile RealmConfiguration realmConfiguration = null;

    Monarchy(RealmConfiguration configuration) {
        this.realmConfiguration = configuration;
    }

    /**
     * Builder class used to build a Monarchy instance.
     *
     * You should only have a singleton instance of Monarchy.
     */
    public static class Builder {
        private RealmConfiguration realmConfiguration;

        public Builder() {
            this.realmConfiguration = Realm.getDefaultConfiguration();
        }

        public Builder setRealmConfiguration(RealmConfiguration realmConfiguration) {
            this.realmConfiguration = realmConfiguration;
            return this;
        }

        public Monarchy build() {
            return new Monarchy(realmConfiguration);
        }
    }

    public final RealmConfiguration getRealmConfiguration() {
        return this.realmConfiguration == null ? getDefaultConfiguration() : this.realmConfiguration;
    }

    public final void runTransactionSync(final Realm.Transaction transaction) {
        doWithRealm(new RealmBlock() {
            @Override
            public void doWithRealm(Realm realm) {
                realm.executeTransaction(transaction);
            }
        });
    }

    private void assertMainThread() {
        if(Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("This method can only be called on the main thread!");
        }
    }

    private HandlerThread handlerThread;
    private Handler handler;
    private AtomicInteger refCount = new AtomicInteger(0);

    private ThreadLocal<Realm> realmThreadLocal = new ThreadLocal<>();
    private ThreadLocal<Map<LiveResults<? extends RealmModel>, RealmResults<? extends RealmModel>>> resultsRefs = new ThreadLocal<Map<LiveResults<? extends RealmModel>, RealmResults<? extends RealmModel>>>() {
        @Override
        protected Map<LiveResults<? extends RealmModel>, RealmResults<? extends RealmModel>> initialValue() {
            return new IdentityHashMap<>();
        }
    };

    <T extends RealmModel> void startListening(final LiveResults<T> liveResults) {
        // build Realm instance
        if(refCount.getAndIncrement() == 0) {
            handlerThread = new HandlerThread("MONARCHY_REALM-#" + hashCode());
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Realm realm = Realm.getInstance(getRealmConfiguration());
                    if(realmThreadLocal.get() == null) {
                        realmThreadLocal.set(realm);
                    }
                }
            });
        }
        // build Realm query
        handler.post(new Runnable() {
            @Override
            public void run() {
                Realm realm = realmThreadLocal.get();
                checkRealmValid(realm);
                RealmResults<T> results = liveResults.createQuery(realm);
                resultsRefs.get().put(liveResults, results);
                results.addChangeListener(new RealmChangeListener<RealmResults<T>>() {
                    @Override
                    public void onChange(@NonNull RealmResults<T> realmResults) {
                        liveResults.updateResults(realmResults.createSnapshot());
                    }
                });
            }
        });
    }

    <T extends RealmModel> void stopListening(final LiveResults<T> liveResults) {
        if(handler == null) {
            return; // edge case, hopefully doesn't happen
        }
        // destroy Realm query
        handler.post(new Runnable() {
            @Override
            public void run() {
                Realm realm = realmThreadLocal.get();
                checkRealmValid(realm);
                RealmResults<? extends RealmModel> realmResults = resultsRefs.get().remove(liveResults);
                if(realmResults != null) {
                    realmResults.removeAllChangeListeners();
                }
            }
        });
        // destroy Realm instance
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(refCount.decrementAndGet() == 0) {
                    Realm realm = realmThreadLocal.get();
                    checkRealmValid(realm);
                    realm.close();
                    if(Realm.getLocalInstanceCount(getRealmConfiguration()) <= 0) {
                        realmThreadLocal.set(null);
                    }
                    handlerThread.quit();
                    handlerThread = null;
                    handler = null;
                }
            }
        });
    }

    private void checkRealmValid(Realm realm) {
        if(realm == null || realm.isClosed()) {
            throw new IllegalStateException("Unexpected state: Realm is not open");
        }
    }

    /**
     * An interface used to define Realm queries, therefore bypassing the thread-local aspect of RealmQuery.
     *
     * @param <T> the realm class
     */
    public interface Query<T extends RealmModel> {
        RealmQuery<T> createQuery(Realm realm);
    }

    /**
     * A mapper interface that can be used in {@link Monarchy#findAllMappedWithChanges(Query, Mapper)} to map out instances on the background looper thread.
     *
     * @param <R> the type to map to
     * @param <T> the type to map from
     */
    public interface Mapper<R, T> {
        R map(T from);
    }

    /**
     * Allows to manually use Realm instance and will automatically close it when done.
     *
     * @param realmBlock the Realm execution block in which Realm should remain open
     */
    public final void doWithRealm(final RealmBlock realmBlock) {
        RealmConfiguration configuration = getRealmConfiguration();
        Realm realm = null;
        try {
            realm = Realm.getInstance(configuration);
            realmBlock.doWithRealm(realm);
        } finally {
            if(realm != null) {
                realm.close();
            }
        }
    }

    /**
     * Writes asynchronously on a single-threaded execution pool.
     *
     * @param transaction the Realm transaction
     */
    public final void writeAsync(final Realm.Transaction transaction) {
        writeScheduler.execute(new Runnable() {
            @Override
            public void run() {
                runTransactionSync(transaction);
            }
        });
    }

    /**
     * Interface to define what to do with the Realm instance.
     */
    public interface RealmBlock {
        void doWithRealm(Realm realm);
    }

    /**
     * Provides ability to synchronously obtain a managed RealmResults as a List, in a safe way.
     *
     * What is actually returned is a snapshot collection.
     *
     * @param realm Realm
     * @param query Query
     * @param <T>   RealmObject type
     * @return the snapshot collection
     */
    public <T extends RealmModel> List<T> findAllSync(Realm realm, Query<T> query) {
        return query.createQuery(realm).findAll().createSnapshot();
    }

    /**
     * Returns a LiveData that evaluates the new results on a background looper thread. The observer receives new data when the database changes.
     *
     * The items are copied out with `realm.copyFromRealm(results)`.
     *
     * @param query  the query
     * @param <T> the RealmModel type
     * @return the LiveData
     */
    public <T extends RealmModel> LiveData<List<T>> findAllCopiedWithChanges(Query<T> query) {
        assertMainThread();
        return new CopiedLiveResults<T>(this, query);
    }

    /**
     * Returns a LiveData that evaluates the new results on a background looper thread. The observer receives new data when the database changes.
     *
     * The items are mapped out with the provided {@link Mapper}.
     *
     * @param query  the query
     * @param <T> the RealmModel type
     * @return the LiveData
     */
    public <T extends RealmModel, U> LiveData<List<U>> findAllMappedWithChanges(Query<T> query, Mapper<U, T> mapper) {
        assertMainThread();
        return new MappedLiveResults<>(this, query, mapper);
    }

    /**
     * Returns a LiveData that evaluates the new results on the UI thread, using Realm's Async API. The observer receives new data when the database changes.
     *
     * The managed change set contains the OrderedCollectionChangeSet evaluated by Realm.
     *
     * @param query the query
     * @param <T> the RealmModel type
     * @return the LiveData
     */
    public <T extends RealmModel> LiveData<ManagedChangeSet<T>> findAllManagedWithChanges(Query<T> query) {
        assertMainThread();
        return new ManagedLiveResults<T>(this, query);
    }
}
