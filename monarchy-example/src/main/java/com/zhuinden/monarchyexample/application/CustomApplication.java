package com.zhuinden.monarchyexample.application;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.application.injection.ApplicationComponent;
import com.zhuinden.monarchyexample.application.injection.DaggerApplicationComponent;

/**
 * Created by Zhuinden on 2017.12.17..
 */

public class CustomApplication
        extends Application {
    private static final String INJECTOR = "INJECTOR";

    ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        Monarchy.init(this);
        applicationComponent = DaggerApplicationComponent.create();
    }

    public ApplicationComponent getInjector() {
        return applicationComponent;
    }

    // context mind tricks
    @Override
    public Object getSystemService(String name) {
        if(name.equals(INJECTOR)) {
            return applicationComponent;
        }
        return super.getSystemService(name);
    }

    @SuppressLint("WrongConstant")
    public static ApplicationComponent getInjector(Context context) {
        // noinspection ResourceType
        return (ApplicationComponent) context.getSystemService(INJECTOR);
    }
}
