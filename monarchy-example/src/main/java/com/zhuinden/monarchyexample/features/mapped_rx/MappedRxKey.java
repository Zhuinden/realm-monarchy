package com.zhuinden.monarchyexample.features.mapped_rx;

import com.google.auto.value.AutoValue;
import com.zhuinden.monarchyexample.utils.BaseFragment;
import com.zhuinden.monarchyexample.utils.BaseKey;

/**
 * Created by Zhuinden on 2017.12.21..
 */

@AutoValue
public abstract class MappedRxKey
        extends BaseKey {
    public static MappedRxKey create() {
        return new AutoValue_MappedRxKey();
    }

    @Override
    protected BaseFragment instantiateFragment() {
        return new MappedRxFragment();
    }
}
