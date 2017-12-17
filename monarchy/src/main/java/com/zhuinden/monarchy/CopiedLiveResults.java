package com.zhuinden.monarchy;

import android.arch.lifecycle.MutableLiveData;

import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * Created by Zhuinden on 2017.12.17..
 */
class CopiedLiveResults<T extends RealmModel> extends MutableLiveData<List<T>> implements LiveResults<T> {
    private final Monarchy monarchy;
    private final Monarchy.Query<T> query;

    private boolean isActive;

    CopiedLiveResults(Monarchy monarchy, Monarchy.Query<T> query) {
        this.monarchy = monarchy;
        this.query = query;
    }

    @Override
    public void onActive() {
        monarchy.onActive();
        monarchy.startListening(this);
        isActive = true;
    }

    @Override
    public void onInactive() {
        isActive = false;
        monarchy.stopListening(this);
        monarchy.onInactive();
    }

    @Override
    public RealmResults<T> createQuery(Realm realm) {
        return query.createQuery(realm).findAllAsync(); // sort/distinct should be handled with new predicate type.
    }

    @Override
    public void updateResults(final OrderedRealmCollection<T> realmCollection) {
        monarchy.doWithRealm(new Monarchy.RealmBlock() {
            @Override
            public void doWithRealm(Realm realm) {
                postValue(realm.copyFromRealm(realmCollection));
            }
        });
    }

    @Override
    protected void finalize()
            throws Throwable {
        super.finalize();
        if(isActive) {
            monarchy.stopListening(this);
            monarchy.onInactive();
            isActive = false;
        }
    }
}
