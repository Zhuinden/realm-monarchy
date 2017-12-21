package com.zhuinden.monarchyexample;

import com.google.auto.value.AutoValue;

/**
 * Created by Zhuinden on 2017.12.17..
 */

@AutoValue
public abstract class Dog {
    public abstract String name();

    public static Dog create(String name) {
        return new AutoValue_Dog(name);
    }
}
