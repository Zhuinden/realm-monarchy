package com.zhuinden.monarchyexample.features.home;

import com.google.auto.value.AutoValue;
import com.zhuinden.monarchyexample.utils.BaseFragment;
import com.zhuinden.monarchyexample.utils.BaseKey;

/**
 * Created by Zhuinden on 2017.12.21..
 */

@AutoValue
public abstract class HomeKey extends BaseKey {
    @Override
    protected BaseFragment instantiateFragment() {
        return new HomeFragment();
    }

    public static HomeKey create() {
        return new AutoValue_HomeKey();
    }
}
