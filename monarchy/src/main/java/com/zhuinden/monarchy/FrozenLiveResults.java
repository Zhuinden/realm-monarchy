package com.zhuinden.monarchy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.lifecycle.MutableLiveData;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * Created by Zhuinden on 2020.06.25.
 */
class FrozenLiveResults<T extends RealmModel> extends MutableLiveData<List<T>>
        implements LiveResults<T> {
    private final Monarchy monarchy;
    private final Monarchy.Query<T> query;

    private boolean isActive;

    FrozenLiveResults(Monarchy monarchy, Monarchy.Query<T> query) {
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
        return query.createQuery(realm).findAllAsync(); // sort/distinct should be handled with new predicate type.
    }

    @Override
    public void updateResults(final OrderedRealmCollection<T> realmCollection) {
        postValue(Collections.unmodifiableList(new ArrayList<>(realmCollection.freeze())));
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
}
