package com.zhuinden.monarchyexample.features.mapped;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.Dog;
import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.RealmDog;
import com.zhuinden.monarchyexample.application.CustomApplication;
import com.zhuinden.monarchyexample.utils.BaseFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Zhuinden on 2017.12.21..
 */

public class MappedFragment
        extends BaseFragment {
    MappedDogAdapter mappedDogAdapter;

    LiveData<List<Dog>> dogs;
    Observer<List<Dog>> observer = dogs -> {
        mappedDogAdapter.updateData(dogs);
    };

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @Inject
    Monarchy monarchy;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        CustomApplication.getInjector(context).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mapped, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        mappedDogAdapter = new MappedDogAdapter();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(mappedDogAdapter);

        dogs = monarchy.findAllMappedWithChanges(realm -> realm.where(RealmDog.class),
                                                 from -> Dog.create(from.getName()));
        dogs.observeForever(observer); // detach != destroy in fragments so this is manual
    }

    @Override
    public void onDestroyView() {
        dogs.removeObserver(observer);
        super.onDestroyView();
    }
}
