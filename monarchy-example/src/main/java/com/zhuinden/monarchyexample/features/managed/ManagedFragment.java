package com.zhuinden.monarchyexample.features.managed;

import android.content.Context;
import android.os.Bundle;

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

import javax.inject.Inject;




/**
 * Created by Zhuinden on 2017.12.21..
 */

public class ManagedFragment
        extends BaseFragment {
    ManagedDogAdapter managedDogAdapter;

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
        recyclerView = view.findViewById(R.id.recycler_view);
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
        managedDogAdapter.notifyDataSetChanged(); // if Realm is closed, then the item count becomes 0. Adapter does not know this while scrolling.
        super.onDestroyView();
    }
}
