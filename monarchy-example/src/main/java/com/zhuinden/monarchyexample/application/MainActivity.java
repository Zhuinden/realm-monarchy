package com.zhuinden.monarchyexample.application;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.RealmDog;
import com.zhuinden.monarchyexample.application.injection.ApplicationComponent;
import com.zhuinden.monarchyexample.features.home.HomeKey;
import com.zhuinden.monarchyexample.utils.BaseKey;
import com.zhuinden.monarchyexample.utils.FragmentStateChanger;
import com.zhuinden.simplestack.BackstackDelegate;
import com.zhuinden.simplestack.HistoryBuilder;
import com.zhuinden.simplestack.StateChange;
import com.zhuinden.simplestack.StateChanger;

import javax.inject.Inject;

import butterknife.ButterKnife;

public class MainActivity
        extends AppCompatActivity
        implements StateChanger {
    private static final String TAG = "MainActivity";

    private boolean isAnimating = false;

    BackstackDelegate backstackDelegate;
    FragmentStateChanger fragmentStateChanger;
    Handler handler = new Handler(Looper.getMainLooper());

    @Inject
    Monarchy monarchy;

    public void navigateTo(BaseKey key) {
        backstackDelegate.getBackstack().goTo(key);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        backstackDelegate = new BackstackDelegate(null);
        backstackDelegate.onCreate(savedInstanceState, getLastCustomNonConfigurationInstance(), HistoryBuilder.single(
                HomeKey.create()));
        backstackDelegate.registerForLifecycleCallbacks(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        fragmentStateChanger = new FragmentStateChanger(getSupportFragmentManager(), R.id.root);

        backstackDelegate.setStateChanger(this);

        ApplicationComponent applicationComponent = CustomApplication.getInjector(this);
        applicationComponent.inject(this);

        // TODO: add RealmDog names enum and add a new one every 1 sec
        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Doge");
        }), 1000);

        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Another doge");
        }), 2000);

        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Pomeranian");
        }), 3000);

        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Husky");
        }), 4000);

        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Malamute");
        }), 5000);

        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Labrador");
        }), 6000);

        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Golden Retriever");
        }), 7000);

        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Poodle");
        }), 8000);

        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Pekingese");
        }), 9000);

        handler.postDelayed(() -> monarchy.writeAsync(realm -> {
            RealmDog dog = realm.createObject(RealmDog.class);
            dog.setName("Boner");
        }), 10000);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return backstackDelegate.onRetainCustomNonConfigurationInstance();
    }

    @Override
    public void onBackPressed() {
        if(!backstackDelegate.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return !isAnimating && super.dispatchTouchEvent(ev);
    }

    @Override
    public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
        if(stateChange.topNewState().equals(stateChange.topPreviousState())) {
            completionCallback.stateChangeComplete();
            return;
        }
        fragmentStateChanger.handleStateChange(stateChange);
        isAnimating = true;
        handler.postDelayed(() -> {
            isAnimating = false;
            completionCallback.stateChangeComplete();
        }, 250);
    }


    // -------------------------------------------------------------------------------------------------
    // I'm actually just bored so I'm setting up what were essentially MortarScopes back in the day, lol
    @Override
    public Object getSystemService(@NonNull String name) {
        if(TAG.equals(name)) {
            return this;
        }
        Object object = super.getSystemService(name);
        if(object == null) {
            // ApplicationContext is not the BaseContext of the Activity so the look-up doesn't work without checking here
            object = getApplicationContext().getSystemService(name);
        }
        return object;
    }

    @SuppressLint("WrongConstant")
    public static MainActivity get(Context context) {
        // noinspection ResourceType
        return (MainActivity) context.getSystemService(TAG);
    }
}
