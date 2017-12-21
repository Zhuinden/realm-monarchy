package com.zhuinden.monarchy;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by Zhuinden on 2017.12.17..
 */

public class Monarchy {
    private final Executor writeScheduler = Executors.newSingleThreadExecutor();

    private static RealmConfiguration invalidDefaultConfig;

    public static void init(Context context) {
        Realm.init(context);
        invalidDefaultConfig = new RealmConfiguration.Builder().build();
        Realm.setDefaultConfiguration(invalidDefaultConfig);
    }

    public static void setDefaultConfiguration(RealmConfiguration realmConfiguration) {
        Realm.setDefaultConfiguration(realmConfiguration);
    }

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

    private RealmConfiguration config() {
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

    public <T extends RealmModel> void startListening(final LiveResults<T> liveResults) {
        // build Realm instance
        if(refCount.getAndIncrement() == 0) {
            handlerThread = new HandlerThread("MONARCHY_REALM-#" + hashCode());
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Realm realm = Realm.getInstance(config());
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

    public <T extends RealmModel> void stopListening(final LiveResults<T> liveResults) {
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
                    if(Realm.getLocalInstanceCount(config()) <= 0) {
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

    public interface Query<T extends RealmModel> {
        RealmQuery<T> createQuery(Realm realm);
    }

    public interface Mapper<R, T> {
        R map(T from);
    }

    public final void doWithRealm(RealmBlock realmBlock) {
        RealmConfiguration configuration = config();
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

    public final void writeAsync(final Realm.Transaction transaction) {
        writeScheduler.execute(new Runnable() {
            @Override
            public void run() {
                runTransactionSync(transaction);
            }
        });
    }

    public interface RealmBlock {
        void doWithRealm(Realm realm);
    }

    public <T extends RealmModel> List<T> findAllSync(Realm realm, Query<T> query) {
        return query.createQuery(realm).findAll().createSnapshot();
    }

    public <T extends RealmModel> LiveData<List<T>> findAllWithChanges(Query<T> query) {
        assertMainThread();
        return new CopiedLiveResults<T>(this, query);
    }

    public <T extends RealmModel, U> LiveData<List<U>> findAllWithChanges(Query<T> query, Mapper<U, T> mapper) {
        assertMainThread();
        return new MappedLiveResults<>(this, query, mapper);
    }
}
