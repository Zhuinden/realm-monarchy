package com.zhuinden.monarchy;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * Created by Zhuinden on 2017.12.17..
 */

interface LiveResults<T extends RealmModel> {
    RealmResults<T> createQuery(Realm realm);
    void updateResults(final OrderedRealmCollection<T> realmResults);
}
