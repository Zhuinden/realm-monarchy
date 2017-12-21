package com.zhuinden.monarchyexample.features.mapped;

import com.google.auto.value.AutoValue;
import com.zhuinden.monarchyexample.utils.BaseFragment;
import com.zhuinden.monarchyexample.utils.BaseKey;

/**
 * Created by Zhuinden on 2017.12.21..
 */

@AutoValue
public abstract class MappedKey
        extends BaseKey {
    public static MappedKey create() {
        return new AutoValue_MappedKey();
    }

    @Override
    protected BaseFragment createFragment() {
        return new MappedFragment();
    }
}
