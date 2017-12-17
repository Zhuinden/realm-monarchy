package com.zhuinden.monarchyexample;

import io.realm.RealmObject;

/**
 * Created by Zhuinden on 2017.12.17..
 */

public class Dog extends RealmObject {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
