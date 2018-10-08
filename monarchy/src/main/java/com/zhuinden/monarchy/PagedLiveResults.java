package com.zhuinden.monarchy;

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.PagedList;

import java.util.concurrent.atomic.AtomicReference;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmResults;

class PagedLiveResults<T extends RealmModel>
        extends MutableLiveData<PagedList<T>>
        implements LiveResults<T> {
    private final Monarchy monarchy;

    private AtomicReference<Monarchy.Query<T>> query;

    private Monarchy.RealmTiledDataSource<T> dataSource;

    private final boolean asAsync;

    private boolean isActive;

    PagedLiveResults(Monarchy monarchy, Monarchy.Query<T> query, boolean asAsync) {
        this.monarchy = monarchy;
        this.query = new AtomicReference<>(query);
        this.asAsync = asAsync;
    }

    @Override
    public void onActive() {
        monarchy.startListening(this);
        isActive = true;
    }

    @Override
    public void onInactive() {
        isActive = false;
        monarchy.stopListening(this);
    }

    @Override
    public RealmResults<T> createQuery(Realm realm) {
        // sort/distinct should be handled with new predicate type.
        if(asAsync) {
            return query.get().createQuery(realm).findAllAsync();
        } else {
            return query.get().createQuery(realm).findAll();
        }
    }

    @Override
    public void updateResults(final OrderedRealmCollection<T> realmCollection) {
        monarchy.doWithRealm(new Monarchy.RealmBlock() {
            @Override
            public void doWithRealm(Realm realm) {
                final Monarchy.RealmTiledDataSource<T> ds = dataSource;
                if(ds != null) {
                    ds.invalidate();
                }
            }
        });
    }

    @Override
    protected void finalize()
            throws Throwable {
        super.finalize();
        if(isActive) {
            monarchy.stopListening(this);
            isActive = false;
        }
    }

    void setDataSource(Monarchy.RealmTiledDataSource<T> dataSource) {
        this.dataSource = dataSource;
    }

    // CALL THIS FROM MONARCHY THREAD
    void updateQuery(Monarchy.Query<T> query) {
        this.query.set(query);
    }

    void invalidateDatasource() {
        if(dataSource != null) {
            dataSource.invalidate();
        }
    }
}
