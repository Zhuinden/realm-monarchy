package com.zhuinden.monarchy;

import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import javax.annotation.Nullable;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * Created by Zhuinden on 2017.12.21..
 */

class ManagedLiveResults<T extends RealmModel>
        extends MutableLiveData<Monarchy.ManagedChangeSet<T>> {
    private final Monarchy monarchy;
    private final Monarchy.Query<T> query;

    private final RealmConfiguration realmConfiguration;

    private OrderedRealmCollectionChangeListener<RealmResults<T>> realmChangeListener = new OrderedRealmCollectionChangeListener<RealmResults<T>>() {
        @Override
        public void onChange(@NonNull RealmResults<T> realmResults, @Nullable OrderedCollectionChangeSet changeSet) {
            postValue(new Monarchy.ManagedChangeSet<>(realmResults, changeSet));
        }
    };
    private Realm realm;
    private RealmResults<T> realmResults;

    public ManagedLiveResults(Monarchy monarchy, Monarchy.Query<T> query) {
        this.monarchy = monarchy;
        this.query = query;

        this.realmConfiguration = this.monarchy.getRealmConfiguration();
    }

    @Override
    protected void onActive() {
        realm = Realm.getInstance(realmConfiguration);
        realmResults = query.createQuery(realm).findAllAsync(); // sort/distinct should be done with new queries
        realmResults.addChangeListener(realmChangeListener);
    }

    @Override
    protected void onInactive() {
        if(realmResults.isValid()) {
            realmResults.removeChangeListener(realmChangeListener);
        }
        realmResults = null;
        realm.close();
        realm = null;
    }
}
