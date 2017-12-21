package com.zhuinden.monarchyexample.features.copied;

import com.google.auto.value.AutoValue;
import com.zhuinden.monarchyexample.utils.BaseFragment;
import com.zhuinden.monarchyexample.utils.BaseKey;

/**
 * Created by Zhuinden on 2017.12.21..
 */

@AutoValue
public abstract class CopiedKey
        extends BaseKey {
    @Override
    protected BaseFragment createFragment() {
        return new CopiedFragment();
    }

    public static CopiedKey create() {
        return new AutoValue_CopiedKey();
    }
}
