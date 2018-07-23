package com.zhuinden.monarchy;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.arch.paging.PositionalDataSource;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
 * In case of `copied`, `mapped`, or `paged` results, the queries are evaluated on a background looper thread.
 *
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
        @NonNull
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

    Monarchy(RealmConfiguration configuration, Executor writeScheduler) {
        this.realmConfiguration = configuration;
        this.writeScheduler = writeScheduler;
    }

    /**
     * Builder class used to build a Monarchy instance.
     *
     * You should only have a singleton instance of Monarchy.
     */
    public static class Builder {
        private RealmConfiguration realmConfiguration;
        private Executor writeScheduler = Executors.newSingleThreadExecutor();

        public Builder() {
            this.realmConfiguration = Realm.getDefaultConfiguration();
        }

        public Builder setRealmConfiguration(RealmConfiguration realmConfiguration) {
            this.realmConfiguration = realmConfiguration;
            return this;
        }

        public Builder setWriteAsyncExecutor(Executor executor) {
            this.writeScheduler = executor;
            return this;
        }

        public Monarchy build() {
            return new Monarchy(realmConfiguration, writeScheduler);
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

    <T extends RealmModel> void startListening(@NonNull final LiveResults<T> liveResults) {
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

    <T extends RealmModel> void stopListening(@NonNull final LiveResults<T> liveResults) {
        Handler handler = this.handler.get();
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
     * This method only makes sense either if Realm is opened manually, or inside a {@link Monarchy#doWithRealm(RealmBlock)} (or {@link Monarchy#runTransactionSync(Realm.Transaction)} method).
     *
     * @param realm Realm
     * @param query Query
     * @param <T>   RealmObject type
     * @return the snapshot collection
     */
    public <T extends RealmModel> List<T> fetchAllManagedSync(Realm realm, Query<T> query) {
        return query.createQuery(realm).findAll().createSnapshot();
    }

    /**
     * Provides ability to synchronously fetch a copied RealmResults.
     *
     * @param query Query
     * @param <T>   RealmObject type
     * @return the copied list
     */
    public <T extends RealmModel> List<T> fetchAllCopiedSync(final Query<T> query) {
        final AtomicReference<List<T>> ref = new AtomicReference<>();
        doWithRealm(new RealmBlock() {
            @Override
            public void doWithRealm(Realm realm) {
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
    public <T extends RealmModel, U> List<U> fetchAllMappedSync(final Query<T> query, final Mapper<U, T> mapper) {
        final AtomicReference<List<U>> ref = new AtomicReference<>();
        doWithRealm(new RealmBlock() {
            @Override
            public void doWithRealm(Realm realm) {
                RealmResults<T> results = query.createQuery(realm).findAll();
                List<U> list = new ArrayList<>(results.size());
                for(T t: results) {
                    list.add(mapper.map(t));
                }
                ref.set(list);
            }
        });
        return Collections.unmodifiableList(ref.get());
    }

    /**
     * Returns a LiveData that evaluates the new results on a background looper thread. The observer receives new data when the database changes.
     *
     * The items are copied out with `realm.copyFromRealm(results)`.
     *
     * @param query the query
     * @param <T>   the RealmModel type
     * @return the LiveData
     */
    public <T extends RealmModel> LiveData<List<T>> findAllCopiedWithChanges(Query<T> query) {
        assertMainThread();
        return new CopiedLiveResults<>(this, query);
    }

    /**
     * Returns a LiveData that evaluates the new results on a background looper thread. The observer receives new data when the database changes.
     *
     * The items are mapped out with the provided {@link Mapper}.
     *
     * @param query the query
     * @param <T>   the RealmModel type
     * @param <U>   the mapped type
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
     * @param <T>   the RealmModel type
     * @return the LiveData
     */
    public <T extends RealmModel> LiveData<ManagedChangeSet<T>> findAllManagedWithChanges(Query<T> query) {
        return findAllManagedWithChanges(query, true);
    }

    /**
     * Returns a LiveData that evaluates the new results on the UI thread, using Realm's Query API. The observer receives new data when the database changes.
     *
     * The managed change set contains the OrderedCollectionChangeSet evaluated by Realm.
     *
     * @param query the query
     * @param <T>   the RealmModel type
     * @param asAsync whether the query should be executed with the Async Query API
     * @return the LiveData
     */
    public <T extends RealmModel> LiveData<ManagedChangeSet<T>> findAllManagedWithChanges(Query<T> query, boolean asAsync) {
        assertMainThread();
        return new ManagedLiveResults<>(this, query, asAsync);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // PAGING
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a DataSource.Factory of (Integer, T) that can be used for creating a paged result set.
     */
    public <T extends RealmModel> RealmDataSourceFactory<T> createDataSourceFactory(Query<T> query) {
        assertMainThread();
        PagedLiveResults<T> liveResults = new PagedLiveResults<T>(this, query);
        return new RealmDataSourceFactory<>(this, liveResults);
    }

    /**
     * Returns a LiveData that evaluates the new results on a background looper thread.
     *
     * The resulting list is driven by a PositionalDataSource from the Paging Library.
     *
     * The fetch executor of the provided LivePagedListBuilder will be overridden with Monarchy's own FetchExecutor that Monarchy runs its queries on.
     */
    public <R, T extends RealmModel> LiveData<PagedList<R>> findAllPagedWithChanges(DataSource.Factory<Integer, T> dataSourceFactory, LivePagedListBuilder<Integer, R> livePagedListBuilder) {
        assertMainThread();
        final MediatorLiveData<PagedList<R>> mediator = new MediatorLiveData<>();
        if(!(dataSourceFactory instanceof RealmDataSourceFactory)) {
            throw new IllegalArgumentException(
                    "The DataSource.Factory provided to this method as the first argument must be the one created by Monarchy.");
        }
        RealmDataSourceFactory<T> realmDataSourceFactory = (RealmDataSourceFactory<T>) dataSourceFactory;
        PagedLiveResults<T> liveResults = realmDataSourceFactory.pagedLiveResults;
        mediator.addSource(liveResults, new Observer<PagedList<T>>() {
            @Override
            public void onChanged(@Nullable PagedList<T> ts) {
                // do nothing, this is to intercept `onActive()` calls to ComputableLiveData
            }
        });
        LiveData<PagedList<R>> computableLiveData = livePagedListBuilder
                .setFetchExecutor(new RealmQueryExecutor(this))
                .build();
        mediator.addSource(computableLiveData, new Observer<PagedList<R>>() {
            @Override
            public void onChanged(@Nullable PagedList<R> data) {
                mediator.postValue(data);
            }
        });
        return mediator;
    }

    private static class RealmQueryExecutor
            implements Executor {
        final Monarchy monarchy;

        public RealmQueryExecutor(Monarchy monarchy) {
            this.monarchy = monarchy;
        }

        @Override
        public void execute(@NonNull Runnable command) {
            Handler handler = monarchy.handler.get();
            if(handler == null) {
                return; // this happens if the gap worker tries to fetch a new page,
                // but the results is no longer observed, and the handler thread is dead.
            }
            if(Looper.myLooper() == handler.getLooper()) {
                command.run();
            } else {
                handler.post(command);
            }
        }
    }

    /**
     * From Paging Library
     */
    static abstract class TiledDataSource<T>
            extends PositionalDataSource<T> {
        @WorkerThread
        public abstract int countItems();

        @WorkerThread
        public abstract List<T> loadRange(int startPosition, int count);

        @Override
        public final void loadInitial(@NonNull LoadInitialParams params,
                                      @NonNull LoadInitialCallback<T> callback) {
            int totalCount = countItems();
            if(totalCount == 0) {
                callback.onResult(Collections.<T>emptyList(), 0, 0);
                return;
            }

            // bound the size requested, based on known count
            final int firstLoadPosition = computeInitialLoadPosition(params, totalCount);
            final int firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount);

            // convert from legacy behavior
            List<T> list = loadRange(firstLoadPosition, firstLoadSize);
            if(list != null && list.size() == firstLoadSize) {
                callback.onResult(list, firstLoadPosition, totalCount);
            } else {
                // null list, or size doesn't match request
                // The size check is a WAR for Room 1.0, subsequent versions do the check in Room
                invalidate();
            }
        }

        @Override
        public final void loadRange(@NonNull LoadRangeParams params,
                                    @NonNull LoadRangeCallback<T> callback) {
            List<T> list = loadRange(params.startPosition, params.loadSize);
            if(list != null) {
                callback.onResult(list);
            } else {
                invalidate();
            }
        }
    }

    static class RealmTiledDataSource<T extends RealmModel>
            extends TiledDataSource<T> {
        final Monarchy monarchy;
        final LiveResults<T> liveResults;

        // WORKER THREAD
        public RealmTiledDataSource(Monarchy monarchy, LiveResults<T> liveResults) {
            this.monarchy = monarchy;
            this.liveResults = liveResults;
        }

        @SuppressWarnings("unchecked")
        @WorkerThread
        public int countItems() {
            Realm realm = monarchy.realmThreadLocal.get();
            RealmResults<T> results = (RealmResults<T>) monarchy.resultsRefs.get().get(liveResults);
            if(realm.isClosed() || results == null || !results.isValid()) {
                return 0;
            }
            return results.size();
        }

        @Override
        public boolean isInvalid() {
            Realm realm = monarchy.realmThreadLocal.get();
            realm.refresh();
            return super.isInvalid();
        }

        @SuppressWarnings("unchecked")
        @WorkerThread
        @Override
        public List<T> loadRange(final int startPosition, final int count) {
            final int countItems = countItems();
            if(countItems == 0) {
                return Collections.emptyList();
            }
            final List<T> list = new ArrayList<>(count);
            monarchy.doWithRealm(new Monarchy.RealmBlock() {
                @Override
                public void doWithRealm(Realm realm) {
                    RealmResults<T> results = (RealmResults<T>) monarchy.resultsRefs.get().get(liveResults);
                    for(int i = startPosition; i < startPosition + count && i < countItems; i++) {
                        // noinspection ConstantConditions
                        list.add(realm.copyFromRealm(results.get(i)));
                    }
                }
            });

            return Collections.unmodifiableList(list);
        }
    }

    /**
     * A DataSource.Factory that handles integration of RealmResults through Paging.
     *
     * @param <T> the type of the RealmModel
     */
    public static final class RealmDataSourceFactory<T extends RealmModel>
            extends DataSource.Factory<Integer, T> {
        Monarchy monarchy;
        final PagedLiveResults<T> pagedLiveResults;

        RealmDataSourceFactory(Monarchy monarchy, PagedLiveResults<T> pagedLiveResults) {
            this.monarchy = monarchy;
            this.pagedLiveResults = pagedLiveResults;
        }

        @Override
        public final DataSource<Integer, T> create() {
            RealmTiledDataSource<T> dataSource = new RealmTiledDataSource<>(monarchy, pagedLiveResults);
            pagedLiveResults.setDataSource(dataSource);
            return dataSource;
        }

        /**
         * Updates the query that the datasource is evaluated by.
         *
         * @param query the query
         */
        public final void updateQuery(Query<T> query) {
            pagedLiveResults.updateQuery(query);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // PAGING END
    ////////////////////////////////////////////////////////////////////////////////
}
