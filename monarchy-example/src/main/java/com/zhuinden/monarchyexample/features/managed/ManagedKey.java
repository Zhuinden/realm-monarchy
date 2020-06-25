package com.zhuinden.monarchyexample.features.managed;

import com.google.auto.value.AutoValue;
import com.zhuinden.monarchyexample.utils.BaseFragment;
import com.zhuinden.monarchyexample.utils.BaseKey;

/**
 * Created by Zhuinden on 2017.12.21..
 */

@AutoValue
public abstract class ManagedKey
        extends BaseKey {
    @Override
    protected BaseFragment instantiateFragment() {
        return new ManagedFragment();
    }

    public static ManagedKey create() {
        return new AutoValue_ManagedKey();
    }
}
