package com.zhuinden.monarchyexample.features.managed;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.RealmDog;
import com.zhuinden.monarchyexample.utils.CustomDiffResult;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Zhuinden on 2017.12.21..
 */

class ManagedDogAdapter
        extends RecyclerView.Adapter<ManagedDogAdapter.ViewHolder> {
    private List<RealmDog> items;

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
        try {
            return items == null ? 0 : items.size();
        } catch(IllegalStateException e) {
            return 0; // I should check `RealmResults.isValid()` here, but I'm using List<T>.
            // RecyclerView is running the prefetch worker on the next Handler loop, but by then, Realm is closed.
        }
    }

    public void updateData(List<RealmDog> items, @Nullable CustomDiffResult diffResult) {
        this.items = items; // RealmResults always sees the same data, so this is ok.
        if(diffResult == null) {
            notifyDataSetChanged();
        } else {
            diffResult.dispatchUpdatesTo(this);
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

