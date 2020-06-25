package com.zhuinden.monarchyexample.utils;

import androidx.recyclerview.widget.RecyclerView;

public interface CustomDiffResult {
    void dispatchUpdatesTo(RecyclerView.Adapter<?> adapter);
}
