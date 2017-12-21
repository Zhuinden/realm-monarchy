package com.zhuinden.monarchyexample.features.mapped_rx;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.LiveDataReactiveStreams;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zhuinden.monarchy.Monarchy;
import com.zhuinden.monarchyexample.Dog;
import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.RealmDog;
import com.zhuinden.monarchyexample.application.CustomApplication;
import com.zhuinden.monarchyexample.utils.BaseFragment;
import com.zhuinden.monarchyexample.utils.DiffCallback;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;


/**
 * Created by Zhuinden on 2017.12.21..
 */

public class MappedRxFragment
        extends BaseFragment {
    MappedRxDogAdapter mappedRxDogAdapter;

    Flowable<List<Dog>> dogs;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @Inject
    Monarchy monarchy;

    Disposable disposable;

    private List<Dog> currentDogs = Collections.emptyList();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        CustomApplication.getInjector(context).inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LiveData<List<Dog>> dogs = monarchy.findAllWithChanges(realm -> realm.where(RealmDog.class),
                                                               from -> Dog.create(from.getName()));
        this.dogs = Flowable.fromPublisher(LiveDataReactiveStreams.toPublisher(getActivity(), dogs));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mapped_rx, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        mappedRxDogAdapter = new MappedRxDogAdapter();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(mappedRxDogAdapter);
        disposable = dogs.map(dogs -> Pair.create(currentDogs, dogs)) //
                .observeOn(Schedulers.computation()) //
                .map(oldAndNewList -> {
                    List<Dog> oldList = oldAndNewList.first;
                    List<Dog> newList = oldAndNewList.second;
                    DiffCallback<Dog> diffCallback = new DiffCallback<Dog>(oldList, newList) {
                        @Override
                        protected boolean areItemsTheSame(Dog oldItem, Dog newItem) {
                            return oldItem.name().equals(newItem.name());
                        }

                        @Override
                        protected boolean areContentsTheSame(Dog oldItem, Dog newItem) {
                            return oldItem.equals(newItem);
                        }
                    };
                    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
                    return Pair.create(newList, diffResult);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSubscriber<Pair<List<Dog>, DiffUtil.DiffResult>>() {
                    @Override
                    public void onNext(Pair<List<Dog>, DiffUtil.DiffResult> listDiffResultPair) {
                        currentDogs = listDiffResultPair.first;
                        mappedRxDogAdapter.updateData(listDiffResultPair.first, listDiffResultPair.second);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposable.dispose();
        disposable = null;
    }
}
