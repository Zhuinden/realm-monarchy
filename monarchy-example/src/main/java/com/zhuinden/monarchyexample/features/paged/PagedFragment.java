package com.zhuinden.monarchyexample.features.paged;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.Dog;
import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.RealmDog;
import com.zhuinden.monarchyexample.RealmDogFields;
import com.zhuinden.monarchyexample.application.CustomApplication;
import com.zhuinden.monarchyexample.utils.BaseFragment;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmQuery;

/**
 * Created by Zhuinden on 2017.12.21..
 */

public class PagedFragment
        extends BaseFragment {
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @BindView(R.id.text_paged_search)
    EditText searchText;

    @Inject
    Monarchy monarchy;

    PagedDogAdapter pagedDogAdapter;
    LiveData<PagedList<Dog>> dogs;
    DataSource.Factory<Integer, Dog> dataSourceFactory;
    Observer<PagedList<Dog>> observer = dogs -> {
        pagedDogAdapter.submitList(dogs);
    };

    Monarchy.RealmDataSourceFactory<RealmDog> realmDataSourceFactory;

    @OnTextChanged(R.id.text_paged_search)
    public void onSearchTextChanged(Editable editable) {
        String text = editable.toString();
        realmDataSourceFactory.updateQuery(realm -> {
            RealmQuery<RealmDog> query = realm.where(RealmDog.class);
            if(text.isEmpty()) {
                return query;
            } else {
                return query.contains(RealmDogFields.NAME, text.trim(), Case.INSENSITIVE);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        CustomApplication.getInjector(context).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paged, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        pagedDogAdapter = new PagedDogAdapter();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(pagedDogAdapter);
        realmDataSourceFactory = monarchy.createDataSourceFactory(
                realm -> realm.where(RealmDog.class));
        dataSourceFactory = realmDataSourceFactory.map(input -> Dog.create(input.getName()));
        dogs = monarchy.findAllPagedWithChanges(realmDataSourceFactory,
                                                new LivePagedListBuilder<>(dataSourceFactory, 20));
        dogs.observeForever(observer); // detach != destroy in fragments so this is manual
    }

    @Override
    public void onDestroyView() {
        dogs.removeObserver(observer);
        super.onDestroyView();
    }
}
