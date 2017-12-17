package com.zhuinden.monarchy;

import android.arch.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * Created by Zhuinden on 2017.12.17..
 */

class MappedLiveResults<T extends RealmModel, U> extends MutableLiveData<List<U>> implements LiveResults<T> {
    private final Monarchy monarchy;
    private final Monarchy.Query<T> query;
    private final Monarchy.Mapper<U, T> mapper;

    private boolean isActive;

    MappedLiveResults(Monarchy monarchy, Monarchy.Query<T> query, Monarchy.Mapper<U, T> mapper) {
        this.monarchy = monarchy;
        this.query = query;
        this.mapper = mapper;
    }

    @Override
    public void onActive() {
        monarchy.onActive();
        monarchy.startListening(this);
        isActive = true;
    }

    @Override
    public void onInactive() {
        monarchy.stopListening(this);
        monarchy.onInactive();
        isActive = false;
    }

    @Override
    public RealmResults<T> createQuery(Realm realm) {
        return query.createQuery(realm).findAllAsync(); // sort/distinct should be handled with new predicate type.
    }

    @Override
    public void updateResults(final OrderedRealmCollection<T> realmResults) {
        monarchy.doWithRealm(new Monarchy.RealmBlock() {
            @Override
            public void doWithRealm(Realm realm) {
                List<U> list = new LinkedList<>();
                for(T t: realmResults) {
                    list.add(mapper.map(t));
                }
                postValue(Collections.unmodifiableList(new ArrayList<U>(list)));
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
