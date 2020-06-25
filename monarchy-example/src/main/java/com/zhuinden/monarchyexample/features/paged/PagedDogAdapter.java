package com.zhuinden.monarchyexample.features.paged;

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zhuinden.monarchyexample.Dog;
import com.zhuinden.monarchyexample.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Zhuinden on 2017.12.21..
 */

class PagedDogAdapter
        extends PagedListAdapter<Dog, PagedDogAdapter.ViewHolder> {
    public PagedDogAdapter() {
        super(Dog.ITEM_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_dog, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Dog dog = getItem(position);
        if(dog != null) {
            holder.bind(dog);
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

        public void bind(Dog dog) {
            textView.setText(dog.name());
        }
    }
}
