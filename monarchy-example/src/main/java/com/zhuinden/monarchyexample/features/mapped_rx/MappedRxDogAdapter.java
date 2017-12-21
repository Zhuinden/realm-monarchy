package com.zhuinden.monarchyexample.features.mapped_rx;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zhuinden.monarchyexample.Dog;
import com.zhuinden.monarchyexample.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Zhuinden on 2017.12.21..
 */

class MappedRxDogAdapter
        extends RecyclerView.Adapter<MappedRxDogAdapter.ViewHolder> {
    private List<Dog> items;

    public MappedRxDogAdapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_dog, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    public void updateData(List<Dog> items, DiffUtil.DiffResult diffResult) {
        this.items = items;
        diffResult.dispatchUpdatesTo(this);
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {
        @BindView(R.id.dog_name)
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(Dog dog) {
            textView.setText(dog.name());
        }
    }
}
