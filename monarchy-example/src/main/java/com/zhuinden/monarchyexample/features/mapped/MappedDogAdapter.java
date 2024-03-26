package com.zhuinden.monarchyexample.features.mapped;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zhuinden.monarchyexample.Dog;
import com.zhuinden.monarchyexample.R;

import java.util.List;




/**
 * Created by Zhuinden on 2017.12.21..
 */

class MappedDogAdapter
        extends RecyclerView.Adapter<MappedDogAdapter.ViewHolder> {
    private List<Dog> items;

    public MappedDogAdapter() {
    }

    @NonNull
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

    @Deprecated
    public void updateData(List<Dog> items) {
        this.items = items;
        notifyDataSetChanged(); // TODO: use DiffUtil
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.dog_name);
        }

        public void bind(Dog dog) {
            textView.setText(dog.name());
        }
    }
}
