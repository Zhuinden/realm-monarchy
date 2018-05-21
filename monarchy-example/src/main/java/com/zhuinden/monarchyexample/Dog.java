package com.zhuinden.monarchyexample;

import android.support.v7.util.DiffUtil;

import com.google.auto.value.AutoValue;

/**
 * Created by Zhuinden on 2017.12.17..
 */

@AutoValue
public abstract class Dog {
    public static DiffUtil.ItemCallback<Dog> ITEM_CALLBACK = new DiffUtil.ItemCallback<Dog>() {
        @Override
        public boolean areItemsTheSame(Dog oldItem, Dog newItem) {
            return oldItem.name().equals(newItem.name());
        }

        @Override
        public boolean areContentsTheSame(Dog oldItem, Dog newItem) {
            return oldItem.name().equals(newItem.name());
        }
    };
    
    public abstract String name();

    public static Dog create(String name) {
        return new AutoValue_Dog(name);
    }
}
