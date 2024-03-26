package com.zhuinden.monarchyexample.features.copied;

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
import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.RealmDog;
import com.zhuinden.monarchyexample.application.CustomApplication;
import com.zhuinden.monarchyexample.utils.BaseFragment;

import java.util.List;

import javax.inject.Inject;




/**
 * Created by Zhuinden on 2017.12.21..
 */

public class CopiedFragment
        extends BaseFragment {
    RecyclerView recyclerView;

    @Inject
    Monarchy monarchy;

    CopiedDogAdapter copiedDogAdapter;
    LiveData<List<RealmDog>> dogs;
    Observer<List<RealmDog>> observer = dogs -> {
        copiedDogAdapter.updateData(dogs);
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        CustomApplication.getInjector(context).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_copied, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recycler_view);
        copiedDogAdapter = new CopiedDogAdapter();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(copiedDogAdapter);

        dogs = monarchy.findAllCopiedWithChanges(realm -> realm.where(RealmDog.class));
        dogs.observeForever(observer); // detach != destroy in fragments so this is manual
    }

    @Override
    public void onDestroyView() {
        dogs.removeObserver(observer);
        super.onDestroyView();
    }
}
