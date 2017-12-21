package com.zhuinden.monarchyexample.features.managed;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.RealmDog;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.OrderedCollectionChangeSet;
import io.realm.RealmResults;

/**
 * Created by Zhuinden on 2017.12.21..
 */

class ManagedDogAdapter
        extends RecyclerView.Adapter<ManagedDogAdapter.ViewHolder> {
    private RealmResults<RealmDog> items;

    public ManagedDogAdapter() {
    }

    @Override
    public ManagedDogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_dog, parent, false));
    }

    @Override
    public void onBindViewHolder(ManagedDogAdapter.ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items == null || !items.isValid() ? 0 : items.size();
    }

    public void updateData(Monarchy.ManagedChangeSet<RealmDog> changes) {
        this.items = changes.getRealmResults(); // RealmResults always sees the same data, so this is ok.

        OrderedCollectionChangeSet changeSet = changes.getOrderedCollectionChangeSet();
        // null Changes means the async query returns the first time.
        if(changeSet == null) {
            notifyDataSetChanged();
            return;
        }
        // For deletions, the adapter has to be notified in reverse order.
        OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
        for(int i = deletions.length - 1; i >= 0; i--) {
            OrderedCollectionChangeSet.Range range = deletions[i];
            notifyItemRangeRemoved(range.startIndex, range.length);
        }

        OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
        for(OrderedCollectionChangeSet.Range range : insertions) {
            notifyItemRangeInserted(range.startIndex, range.length);
        }

        OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
        for(OrderedCollectionChangeSet.Range range : modifications) {
            notifyItemRangeChanged(range.startIndex, range.length);
        }
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {
        @BindView(R.id.dog_name)
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(RealmDog dog) {
            textView.setText(dog.getName());
        }
    }
}

