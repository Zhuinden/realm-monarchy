package com.zhuinden.monarchyexample.utils;

import java.util.List;

import androidx.recyclerview.widget.DiffUtil;

/**
 * Created by Zhuinden on 2017.12.21..
 */
public abstract class DiffCallback<T>
        extends DiffUtil.Callback {
    private final List<T> oldList;
    private final List<T> newList;

    public DiffCallback(List<T> oldList, List<T> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public final int getOldListSize() {
        return oldList == null ? 0 : oldList.size();
    }

    @Override
    public final int getNewListSize() {
        return newList == null ? 0 : newList.size();
    }

    protected abstract boolean areItemsTheSame(T oldItem, T newItem);

    protected abstract boolean areContentsTheSame(T oldItem, T newItem);

    @Override
    public final boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return areItemsTheSame(oldList.get(oldItemPosition), newList.get(newItemPosition));
    }

    @Override
    public final boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return areContentsTheSame(oldList.get(oldItemPosition), newList.get(newItemPosition));
    }
}
