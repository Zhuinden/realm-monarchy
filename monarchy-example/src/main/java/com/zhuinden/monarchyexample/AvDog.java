package com.zhuinden.monarchyexample;

import com.google.auto.value.AutoValue;

/**
 * Created by Zhuinden on 2017.12.17..
 */

@AutoValue
public abstract class AvDog {
    public abstract String name();

    public static AvDog create(String name) {
        return new AutoValue_AvDog(name);
    }
}
