package com.zhuinden.monarchyexample.features.managed;

import android.support.v7.widget.RecyclerView;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.utils.CustomDiffResult;

import io.realm.OrderedCollectionChangeSet;
import io.realm.RealmModel;

public class MonarchyDiffResult<T extends RealmModel>
        implements CustomDiffResult {
    private final Monarchy.ManagedChangeSet<T> changes;

    public MonarchyDiffResult(Monarchy.ManagedChangeSet<T> changes) {
        this.changes = changes;
    }

    @Override
    public void dispatchUpdatesTo(RecyclerView.Adapter<?> adapter) {
        OrderedCollectionChangeSet changeSet = changes.getOrderedCollectionChangeSet();
        // null Changes means the async query returns the first time.
        if(changeSet.getState() == OrderedCollectionChangeSet.State.INITIAL) {
            adapter.notifyDataSetChanged();
            return;
        }
        // For deletions, the adapter has to be notified in reverse order.
        OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
        for(int i = deletions.length - 1; i >= 0; i--) {
            OrderedCollectionChangeSet.Range range = deletions[i];
            adapter.notifyItemRangeRemoved(range.startIndex, range.length);
        }

        OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
        for(OrderedCollectionChangeSet.Range range : insertions) {
            adapter.notifyItemRangeInserted(range.startIndex, range.length);
        }

        OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
        for(OrderedCollectionChangeSet.Range range : modifications) {
            adapter.notifyItemRangeChanged(range.startIndex, range.length);
        }
    }
}
