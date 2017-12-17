package com.zhuinden.monarchyexample;

import android.app.Application;

import com.zhuinden.monarchy.Monarchy;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Zhuinden on 2017.12.17..
 */

public class CustomApplication
        extends Application {
    Monarchy monarchy;

    @Override
    public void onCreate() {
        super.onCreate();
        Monarchy.init(this);
        monarchy = new Monarchy.Builder()
                .setRealmConfiguration(new RealmConfiguration.Builder()
                                               .deleteRealmIfMigrationNeeded()
                                               .initialData(realm -> {
                                                   Dog dog = realm.createObject(Dog.class);
                                                   dog.setName("Corgi");
                                               })
                                               .build())
                .build();
    }

    public Monarchy getMonarchy() {
        return monarchy;
    }
}
