package com.zhuinden.monarchyexample.utils;

import android.os.Bundle;
import android.os.Parcelable;

import com.zhuinden.simplestackextensions.fragments.DefaultFragmentKey;

/**
 * Created by Owner on 2017. 06. 29..
 */

public abstract class BaseKey extends DefaultFragmentKey
        implements Parcelable {
    @Override
    public String getFragmentTag() {
        return toString();
    }
}
