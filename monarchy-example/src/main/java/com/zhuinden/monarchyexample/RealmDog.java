package com.zhuinden.monarchyexample;

import androidx.recyclerview.widget.DiffUtil;
import io.realm.RealmObject;

/**
 * Created by Zhuinden on 2017.12.17..
 */

public class RealmDog
        extends RealmObject {
    public static DiffUtil.ItemCallback<RealmDog> ITEM_CALLBACK = new DiffUtil.ItemCallback<RealmDog>() {
        @Override
        public boolean areItemsTheSame(RealmDog oldItem, RealmDog newItem) {
            return oldItem.name.equals(newItem.name);
        }

        @Override
        public boolean areContentsTheSame(RealmDog oldItem, RealmDog newItem) {
            return oldItem.name.equals(newItem.name);
        }
    };

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
