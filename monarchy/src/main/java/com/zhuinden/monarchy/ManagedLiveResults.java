package com.zhuinden.monarchy;

import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import javax.annotation.Nullable;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by Zhuinden on 2017.12.21..
 */

class ManagedLiveResults<T extends RealmModel>
        extends MutableLiveData<Monarchy.ManagedChangeSet<T>> {
    private static class EmptyChangeSet implements OrderedCollectionChangeSet {
        private static final int[] NO_INDEX_CHANGES = new int[0];
        private static final Range[] NO_RANGE_CHANGES = new Range[0];
        
        @Override
        public State getState() {
            return State.INITIAL;
        }

        @Override
        public int[] getDeletions() {
            return NO_INDEX_CHANGES;
        }

        @Override
        public int[] getInsertions() {
            return NO_INDEX_CHANGES;
        }

        @Override
        public int[] getChanges() {
            return NO_INDEX_CHANGES;
        }

        @Override
        public Range[] getDeletionRanges() {
            return NO_RANGE_CHANGES;
        }

        @Override
        public Range[] getInsertionRanges() {
            return NO_RANGE_CHANGES;
        }

        @Override
        public Range[] getChangeRanges() {
            return NO_RANGE_CHANGES;
        }

        @Nullable
        @Override
        public Throwable getError() {
            return null;
        }

        @Override
        public boolean isCompleteResult() {
            return true;
        }
    }
    
    private final Monarchy monarchy;
    private final Monarchy.Query<T> query;
    private final boolean asAsync;

    private final RealmConfiguration realmConfiguration;

    private OrderedRealmCollectionChangeListener<RealmResults<T>> realmChangeListener = new OrderedRealmCollectionChangeListener<RealmResults<T>>() {
        @Override
        public void onChange(@NonNull RealmResults<T> realmResults, @NonNull OrderedCollectionChangeSet changeSet) {
            Monarchy.ManagedChangeSet<T> managedChangeSet = new Monarchy.ManagedChangeSet<>(realmResults, changeSet);
            if(!asAsync && changeSet.getState() == OrderedCollectionChangeSet.State.INITIAL) {
                setValue(managedChangeSet);
            } else {
                postValue(managedChangeSet);
            }
        }
    };
    private Realm realm;
    private RealmResults<T> realmResults;

    public ManagedLiveResults(Monarchy monarchy, Monarchy.Query<T> query, boolean asAsync) {
        this.monarchy = monarchy;
        this.query = query;
        this.asAsync = asAsync;

        this.realmConfiguration = this.monarchy.getRealmConfiguration();
    }

    @Override
    protected void onActive() {
        realm = Realm.getInstance(realmConfiguration);
        RealmQuery<T> realmQuery = query.createQuery(realm);
        if(asAsync) {
            realmResults = realmQuery.findAllAsync(); // sort/distinct should be done with new queries, 5.0+
        } else {
            realmResults = realmQuery.findAll(); // sort/distinct should be done with new queries, 5.0+
        }
        realmResults.addChangeListener(realmChangeListener);

        if(!asAsync) {
            setValue(new Monarchy.ManagedChangeSet<T>(realmResults, new EmptyChangeSet()));
        }
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
