package com.zhuinden.monarchyexample.features.frozen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.RealmDog;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Zhuinden on 2020.06.25.
 */

class FrozenDogAdapter
        extends RecyclerView.Adapter<FrozenDogAdapter.ViewHolder> {
    private List<RealmDog> items;

    public FrozenDogAdapter() {
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
    public void updateData(List<RealmDog> items) {
        this.items = items;
        notifyDataSetChanged(); // TODO: use DiffUtil
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
