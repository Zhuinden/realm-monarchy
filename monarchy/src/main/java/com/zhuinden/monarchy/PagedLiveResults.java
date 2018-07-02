package com.zhuinden.monarchy;

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.PagedList;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmResults;

class PagedLiveResults<T extends RealmModel>
        extends MutableLiveData<PagedList<T>>
        implements LiveResults<T> {
    private final Monarchy monarchy;

    private Monarchy.Query<T> query;

    private Monarchy.RealmTiledDataSource<T> dataSource;

    private boolean isActive;

    PagedLiveResults(Monarchy monarchy, Monarchy.Query<T> query) {
        this.monarchy = monarchy;
        this.query = query;
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
        return query.createQuery(realm).findAll(); // sort/distinct should be handled with new predicate type.
        // paged results must be based on synchronous query!
    }

    @Override
    public void updateResults(final OrderedRealmCollection<T> realmCollection) {
        monarchy.doWithRealm(new Monarchy.RealmBlock() {
            @Override
            public void doWithRealm(Realm realm) {
                dataSource.invalidate();
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

    public void updateQuery(Monarchy.Query<T> query) {
        this.query = query;
        dataSource.invalidate();
    }
}
