package com.zhuinden.monarchyexample.features.paged;

import com.google.auto.value.AutoValue;
import com.zhuinden.monarchyexample.utils.BaseFragment;
import com.zhuinden.monarchyexample.utils.BaseKey;

/**
 * Created by Zhuinden on 2017.12.21..
 */

@AutoValue
public abstract class PagedKey
        extends BaseKey {
    @Override
    protected BaseFragment createFragment() {
        return new PagedFragment();
    }

    public static PagedKey create() {
        return new AutoValue_PagedKey();
    }
}
