package com.zhuinden.monarchyexample.features.managed;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.RealmDog;
import com.zhuinden.monarchyexample.application.CustomApplication;
import com.zhuinden.monarchyexample.utils.BaseFragment;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Zhuinden on 2017.12.21..
 */

public class ManagedFragment
        extends BaseFragment {
    ManagedDogAdapter managedDogAdapter;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @Inject
    Monarchy monarchy;

    LiveData<Monarchy.ManagedChangeSet<RealmDog>> changes;
    Observer<Monarchy.ManagedChangeSet<RealmDog>> observer = changes -> {
        if(changes != null) {
            managedDogAdapter.updateData(changes.getRealmResults(), new MonarchyDiffResult<>(changes));
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        CustomApplication.getInjector(context).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_managed, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        managedDogAdapter = new ManagedDogAdapter();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(managedDogAdapter);

        changes = monarchy.findAllManagedWithChangesSync(realm -> realm.where(RealmDog.class));
        changes.observeForever(observer); // detach != destroy in fragments so this is manual
    }

    @Override
    public void onDestroyView() {
        changes.removeObserver(observer);
        super.onDestroyView();
    }
}
