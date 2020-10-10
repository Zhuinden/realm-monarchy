package com.zhuinden.monarchy;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import io.realm.OrderedCollectionChangeSet;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmQuery;
import io.realm.RealmResults;


/**
 * Monarchy is a wrapper around Realm that simplifies its lifecycle management, and leverages the power of LiveData.
 * <p>
 * Not only does it auto-manage the Realm lifecycle, but specifically prevents the ability to use Realm in such a way that it is impossible to leave a Realm instance open by accident.
 * <p>
 * In case of `copied`, `mapped`, or `paged` results, the queries are evaluated on a background looper thread.
 * <p>
 * In case of `managed` results, the RealmResults is provided along with its change set.
 */
public final class Monarchy {
    private final Object LOCK = new Object();

    /**
     * A class that contains the RealmResults and the OrderedCollectionChangeSet.
     *
     * @param <T> the RealmModel type
     */
    public static class ManagedChangeSet<T extends RealmModel> {
        private final RealmResults<T> realmResults;
        private final OrderedCollectionChangeSet orderedCollectionChangeSet;

        ManagedChangeSet(@Nonnull RealmResults<T> realmResults, @Nonnull OrderedCollectionChangeSet orderedCollectionChangeSet) {
            this.realmResults = realmResults;
            this.orderedCollectionChangeSet = orderedCollectionChangeSet;
        }

        /**
         * Gets the RealmResults.
         *
         * @return the RealmResults
         */
        @Nonnull
        public RealmResults<T> getRealmResults() {
            return realmResults;
        }

        /**
         * Gets the ordered collection change set.
         *
         * @return the change set
         */
        @Nonnull
        public OrderedCollectionChangeSet getOrderedCollectionChangeSet() {
            return orderedCollectionChangeSet;
        }
    }

    private final Executor writeScheduler;

    private static volatile RealmConfiguration invalidDefaultConfig;

    /**
     * Initializes Realm as usual, except sets a default configuration to detect if a custom default is properly set.
     *
     * @param context app context
     */
    public static void init(@Nonnull Context context) {
        Realm.init(context);
        invalidDefaultConfig = new RealmConfiguration.Builder().build();
        Realm.setDefaultConfiguration(invalidDefaultConfig);
    }

    /**
     * Calls Realm.setDefaultConfiguration(config).
     *
     * @param realmConfiguration realm configuration
     */
    public static void setDefaultConfiguration(@Nonnull RealmConfiguration realmConfiguration) {
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    /**
     * Returns the default configuration.
     *
     * @return the custom default configuration
     * @throws IllegalStateException if the invalid default configuration is still set.
     */
    @Nonnull
    public static RealmConfiguration getDefaultConfiguration() {
        final RealmConfiguration config = Realm.getDefaultConfiguration();
        if(config == invalidDefaultConfig || config == null) {
            throw new IllegalStateException("No default configuration is set!");
        }
        return config;
    }

    private volatile RealmConfiguration realmConfiguration = null;

    Monarchy(@Nonnull RealmConfiguration configuration, @Nonnull Executor writeScheduler) {
        this.realmConfiguration = configuration;
        this.writeScheduler = writeScheduler;
    }

    /**
     * Builder class used to build a Monarchy instance.
     * <p>
     * You should only have a singleton instance of Monarchy.
     */
    public static class Builder {
        private RealmConfiguration realmConfiguration;
        private Executor writeScheduler = Executors.newSingleThreadExecutor();

        public Builder() {
            this.realmConfiguration = Realm.getDefaultConfiguration();
        }

        @Nonnull
        public Builder setRealmConfiguration(@Nullable RealmConfiguration realmConfiguration) {
            this.realmConfiguration = realmConfiguration;
            return this;
        }

        @Nonnull
        public Builder setWriteAsyncExecutor(@Nonnull Executor executor) {
            //noinspection ConstantConditions
            if(executor == null) {
                throw new IllegalArgumentException("executor should not be null!");
            }
            this.writeScheduler = executor;
            return this;
        }

        @Nonnull
        public Monarchy build() {
            return new Monarchy(realmConfiguration, writeScheduler);
        }
    }

    @Nonnull
    public final RealmConfiguration getRealmConfiguration() {
        return this.realmConfiguration == null ? getDefaultConfiguration() : this.realmConfiguration;
    }

    public final void runTransactionSync(@Nonnull final Realm.Transaction transaction) {
        doWithRealm(new RealmBlock() {
            @Override
            public void doWithRealm(@NonNull Realm realm) {
                realm.executeTransaction(transaction);
            }
        });
    }

    private void assertMainThread() {
        if(Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("This method can only be called on the main thread!");
        }
    }

    private AtomicReference<HandlerThread> handlerThread = new AtomicReference<>();
    private AtomicReference<Handler> handler = new AtomicReference<>();
    private AtomicInteger refCount = new AtomicInteger(0);

    private ThreadLocal<Realm> realmThreadLocal = new ThreadLocal<>();
    private ThreadLocal<Map<LiveResults<? extends RealmModel>, RealmResults<? extends RealmModel>>> resultsRefs = new ThreadLocal<Map<LiveResults<? extends RealmModel>, RealmResults<? extends RealmModel>>>() {
        @Override
        protected Map<LiveResults<? extends RealmModel>, RealmResults<? extends RealmModel>> initialValue() {
            return new IdentityHashMap<>();
        }
    };

    // CALL THIS SYNC ON MONARCHY THREAD
    <T extends RealmModel> void createAndObserveRealmQuery(@Nullable final LiveResults<T> liveResults) {
        Realm realm = realmThreadLocal.get();
        checkRealmValid(realm);
        if(liveResults == null) {
            return;
        }
        RealmResults<T> results = liveResults.createQuery(realm);
        resultsRefs.get().put(liveResults, results);
        results.addChangeListener(new RealmChangeListener<RealmResults<T>>() {
            @Override
            public void onChange(@Nonnull RealmResults<T> realmResults) {
                liveResults.updateResults(realmResults);
            }
        });
    }

    // CALL THIS SYNC ON MONARCHY THREAD
    <T extends RealmModel> void destroyRealmQuery(@Nullable final LiveResults<T> liveResults) {
        Realm realm = realmThreadLocal.get();
        checkRealmValid(realm);
        if(liveResults == null) {
            return;
        }
        RealmResults<? extends RealmModel> realmResults = resultsRefs.get().remove(liveResults);
        if(realmResults != null) {
            realmResults.removeAllChangeListeners();
        }
    }

    <T extends RealmModel> void startListening(@Nullable final LiveResults<T> liveResults) {
        // build Realm instance
        if(refCount.getAndIncrement() == 0) {
            synchronized(LOCK) {
                HandlerThread handlerThread = new HandlerThread("MONARCHY_REALM-#" + hashCode());
                handlerThread.start();
                Handler handler = new Handler(handlerThread.getLooper());
                this.handlerThread.set(handlerThread);
                this.handler.set(handler);
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
        }

        // build Realm query
        handler.get().post(new Runnable() {
            @Override
            public void run() {
                createAndObserveRealmQuery(liveResults);
            }
        });
    }

    <T extends RealmModel> void stopListening(@Nullable final LiveResults<T> liveResults) {
        Handler handler = this.handler.get();
        if(handler == null) {
            return; // edge case, hopefully doesn't happen
        }
        // destroy Realm query
        handler.post(new Runnable() {
            @Override
            public void run() {
                destroyRealmQuery(liveResults);
            }
        });
        // destroy Realm instance
        handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized(LOCK) {
                    if(refCount.decrementAndGet() == 0) {
                        Realm realm = realmThreadLocal.get();
                        checkRealmValid(realm);
                        realm.close();
                        if(Realm.getLocalInstanceCount(getRealmConfiguration()) <= 0) {
                            realmThreadLocal.set(null);
                        }
                        HandlerThread handlerThread = Monarchy.this.handlerThread.getAndSet(null);
                        Monarchy.this.handler.set(null);
                        handlerThread.quit();
                    }
                }
            }
        });
    }

    private void checkRealmValid(@Nullable Realm realm) {
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
        @Nonnull
        RealmQuery<T> createQuery(@Nonnull Realm realm);
    }

    /**
     * A mapper interface that can be used in {@link Monarchy#findAllMappedWithChanges(Query, Mapper)} to map out instances on the background looper thread.
     *
     * @param <R> the type to map to
     * @param <T> the type to map from
     */
    public interface Mapper<R, T> {
        // R is platform type on purpose
        R map(@Nonnull T from);
    }

    /**
     * Allows to manually use Realm instance and will automatically close it when done.
     *
     * @param realmBlock the Realm execution block in which Realm should remain open
     */
    public final void doWithRealm(@Nonnull final RealmBlock realmBlock) {
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
    public final void writeAsync(@Nonnull final Realm.Transaction transaction) {
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
        void doWithRealm(@Nonnull Realm realm);
    }

    /**
     * Provides ability to synchronously obtain a managed RealmResults as a List, in a safe way.
     * <p>
     * What is actually returned is a snapshot collection.
     * <p>
     * This method only makes sense either if Realm is opened manually, or inside a {@link Monarchy#doWithRealm(RealmBlock)} (or {@link Monarchy#runTransactionSync(Realm.Transaction)} method).
     *
     * @param realm Realm
     * @param query Query
     * @param <T>   RealmObject type
     * @return the snapshot collection
     */
    @Nonnull
    public <T extends RealmModel> List<T> fetchAllManagedSync(@Nonnull Realm realm, @Nonnull Query<T> query) {
        return query.createQuery(realm).findAll().createSnapshot();
    }

    /**
     * Provides ability to synchronously fetch a copied RealmResults.
     *
     * @param query Query
     * @param <T>   RealmObject type
     * @return the copied list
     */
    @Nonnull
    public <T extends RealmModel> List<T> fetchAllCopiedSync(@Nonnull final Query<T> query) {
        final AtomicReference<List<T>> ref = new AtomicReference<>();
        doWithRealm(new RealmBlock() {
            @Override
            public void doWithRealm(@NonNull Realm realm) {
                ref.set(realm.copyFromRealm(query.createQuery(realm).findAll()));
            }
        });
        return Collections.unmodifiableList(ref.get());
    }

    /**
     * Provides ability to synchronously fetch a mapped RealmResults.
     *
     * @param query Query
     * @param <T>   RealmObject type
     * @param <U>   the mapped type
     * @return the copied list
     */
    @Nonnull
    public <T extends RealmModel, U> List<U> fetchAllMappedSync(@Nonnull final Query<T> query, @Nonnull final Mapper<U, T> mapper) {
        final AtomicReference<List<U>> ref = new AtomicReference<>();
        doWithRealm(new RealmBlock() {
            @Override
            public void doWithRealm(@NonNull Realm realm) {
                RealmResults<T> results = query.createQuery(realm).findAll();
                List<U> list = new ArrayList<>(results.size());
                for(T t : results) {
                    list.add(mapper.map(t));
                }
                ref.set(list);
            }
        });
        return Collections.unmodifiableList(ref.get());
    }

    /**
     * Returns a LiveData that evaluates the new results on a background looper thread. The observer receives new data when the database changes.
     * <p>
     * The items are copied out with `realm.copyFromRealm(results)`.
     *
     * @param query the query
     * @param <T>   the RealmModel type
     * @return the LiveData
     */
    @Nonnull
    public <T extends RealmModel> LiveData<List<T>> findAllCopiedWithChanges(@Nonnull Query<T> query) {
        assertMainThread();
        return new CopiedLiveResults<>(this, query, Integer.MAX_VALUE);
    }

    /**
     * Returns a LiveData that evaluates the new results on a background looper thread. The observer receives new data when the database changes.
     * <p>
     * The items are copied out with `realm.copyFromRealm(results, maxDepth)`.
     *
     * @param maxDepth the max depth
     * @param query    the query
     * @param <T>      the RealmModel type
     * @return the LiveData
     */
    @Nonnull
    public <T extends RealmModel> LiveData<List<T>> findAllCopiedWithChanges(int maxDepth, @Nonnull Query<T> query) {
        assertMainThread();
        return new CopiedLiveResults<>(this, query, maxDepth);
    }

    /**
     * Returns a LiveData that evaluates the new results on a background looper thread. The observer receives new data when the database changes.
     * <p>
     * The items are frozen with `realmResults.freeze()`.
     *
     * @param query the query
     * @param <T>   the RealmModel type
     * @return the LiveData
     */
    @Nonnull
    public <T extends RealmModel> LiveData<List<T>> findAllFrozenWithChanges(@Nonnull Query<T> query) {
        assertMainThread();
        return new FrozenLiveResults<>(this, query);
    }

    /**
     * Returns a LiveData that evaluates the new results on a background looper thread. The observer receives new data when the database changes.
     * <p>
     * The items are mapped out with the provided {@link Mapper}.
     *
     * @param query the query
     * @param <T>   the RealmModel type
     * @param <U>   the mapped type
     * @return the LiveData
     */
    @Nonnull
    public <T extends RealmModel, U> LiveData<List<U>> findAllMappedWithChanges(@Nonnull Query<T> query, @Nonnull Mapper<U, T> mapper) {
        assertMainThread();
        return new MappedLiveResults<>(this, query, mapper);
    }

    @Nonnull
    private <T extends RealmModel> LiveData<ManagedChangeSet<T>> findAllManagedWithChanges(@Nonnull Query<T> query, boolean asAsync) {
        assertMainThread();
        return new ManagedLiveResults<>(this, query, asAsync);
    }

    /**
     * Returns a LiveData that evaluates the new results on the UI thread, using Realm's Async Query API. The observer receives new data when the database changes.
     * <p>
     * The managed change set contains the OrderedCollectionChangeSet evaluated by Realm.
     *
     * @param query the query
     * @param <T>   the RealmModel type
     * @return the LiveData
     */
    @Nonnull
    public <T extends RealmModel> LiveData<ManagedChangeSet<T>> findAllManagedWithChanges(@Nonnull Query<T> query) {
        return findAllManagedWithChanges(query, true);
    }

    /**
     * Returns a LiveData that evaluates the new results on the UI thread, using Realm's Sync Query API. The observer receives new data when the database changes.
     * <p>
     * The managed change set contains the OrderedCollectionChangeSet evaluated by Realm.
     *
     * @param query the query
     * @param <T>   the RealmModel type
     * @return the LiveData
     */
    @Nonnull
    public <T extends RealmModel> LiveData<ManagedChangeSet<T>> findAllManagedWithChangesSync(@Nonnull Query<T> query) {
        return findAllManagedWithChanges(query, false);
    }

    private final AtomicBoolean isForcedOpen = new AtomicBoolean(false);

    /**
     * Forcefully opens the Monarchy thread, keeping it alive until {@link Monarchy#closeManually()} is called.
     */
    public void openManually() {
        if(isForcedOpen.compareAndSet(false, true)) {
            startListening(null);
        } else {
            throw new IllegalStateException("The Monarchy thread is already forced open.");
        }
    }

    /**
     * If the Monarchy thread was opened manually, then this method can be used to decrement the forced reference count increment.
     * <p>
     * This means that the Monarchy thread does not stop unless all observed LiveData are also inactive.
     */
    public void closeManually() {
        if(isForcedOpen.compareAndSet(true, false)) {
            stopListening(null);
        } else {
            throw new IllegalStateException(
                    "Cannot close Monarchy thread manually if it was not opened manually.");
        }
    }

    /**
     * Returns if the Monarchy thread is open.
     *
     * @return if the monarchy thread is open
     */
    public boolean isMonarchyThreadOpen() {
        synchronized(LOCK) {
            return handler.get() != null;
        }
    }

    /**
     * Posts the RealmBlock to the Monarchy thread, and executes it there.
     *
     * @param realmBlock the Realm block
     * @throws IllegalStateException if the Monarchy thread is not open
     */
    public void postToMonarchyThread(@Nonnull final RealmBlock realmBlock) {
        final Handler _handler = handler.get();
        if(_handler == null) {
            throw new IllegalStateException(
                    "Cannot post to Monarchy thread when the Monarchy thread is not open.");
        } else {
            _handler.post(new Runnable() {
                @Override
                public void run() {
                    Realm realm = realmThreadLocal.get();
                    checkRealmValid(realm);
                    realmBlock.doWithRealm(realm);
                }
            });
        }
    }
}
