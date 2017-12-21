package com.zhuinden.monarchyexample.application.injection;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.application.MainActivity;
import com.zhuinden.monarchyexample.features.copied.CopiedFragment;
import com.zhuinden.monarchyexample.features.home.HomeFragment;
import com.zhuinden.monarchyexample.features.managed.ManagedFragment;
import com.zhuinden.monarchyexample.features.mapped.MappedFragment;

import javax.inject.Singleton;

import dagger.Component;
import io.realm.RealmConfiguration;

/**
 * Created by Zhuinden on 2017.12.21..
 */

@Singleton
@Component(modules = DataModule.class)
public interface ApplicationComponent {
    RealmConfiguration realmConfiguration();

    Monarchy monarchy();

    void inject(MainActivity mainActivity);

    void inject(CopiedFragment copiedFragment);

    void inject(HomeFragment homeFragment);

    void inject(ManagedFragment managedFragment);

    void inject(MappedFragment mappedFragment);
}
