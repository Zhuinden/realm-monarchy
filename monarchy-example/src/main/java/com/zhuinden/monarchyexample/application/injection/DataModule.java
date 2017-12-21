package com.zhuinden.monarchyexample.application.injection;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.RealmDog;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.realm.RealmConfiguration;

/**
 * Created by Zhuinden on 2017.12.21..
 */

@Module
public class DataModule {
    @Provides
    @Singleton
    RealmConfiguration realmConfiguration() {
        return new RealmConfiguration.Builder() //
                .deleteRealmIfMigrationNeeded() //
                .initialData(realm -> { //
                    RealmDog dog = realm.createObject(RealmDog.class);
                    dog.setName("Corgi");
                }).build();
    }

    @Provides
    @Singleton
    Monarchy monarchy(RealmConfiguration realmConfiguration) {
        return new Monarchy.Builder() //
                .setRealmConfiguration(realmConfiguration) //
                .build();
    }
}
