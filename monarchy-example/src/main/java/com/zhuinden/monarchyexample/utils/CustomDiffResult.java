package com.zhuinden.monarchyexample.utils;

import android.support.v7.widget.RecyclerView;

public interface CustomDiffResult {
    void dispatchUpdatesTo(RecyclerView.Adapter<?> adapter);
}
