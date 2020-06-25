package com.zhuinden.monarchyexample.features.frozen;

import com.google.auto.value.AutoValue;
import com.zhuinden.monarchyexample.utils.BaseFragment;
import com.zhuinden.monarchyexample.utils.BaseKey;

/**
 * Created by Zhuinden on 2020.06.25.
 */

@AutoValue
public abstract class FrozenKey
        extends BaseKey {
    @Override
    protected BaseFragment instantiateFragment() {
        return new FrozenFragment();
    }

    public static FrozenKey create() {
        return new AutoValue_FrozenKey();
    }
}
