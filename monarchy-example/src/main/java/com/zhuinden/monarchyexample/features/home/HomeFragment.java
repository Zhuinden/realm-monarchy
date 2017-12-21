package com.zhuinden.monarchyexample.features.home;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zhuinden.monarchyexample.R;
import com.zhuinden.monarchyexample.application.CustomApplication;
import com.zhuinden.monarchyexample.application.MainActivity;
import com.zhuinden.monarchyexample.features.copied.CopiedKey;
import com.zhuinden.monarchyexample.features.managed.ManagedKey;
import com.zhuinden.monarchyexample.features.mapped.MappedKey;
import com.zhuinden.monarchyexample.features.mapped_rx.MappedRxKey;
import com.zhuinden.monarchyexample.utils.BaseFragment;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Zhuinden on 2017.12.21..
 */

public class HomeFragment
        extends BaseFragment {
    @OnClick(R.id.button_copied)
    public void onCopied(View view) {
        MainActivity.get(view.getContext()).navigateTo(CopiedKey.create());
    }

    @OnClick(R.id.button_managed)
    public void onManaged(View view) {
        MainActivity.get(view.getContext()).navigateTo(ManagedKey.create());
    }

    @OnClick(R.id.button_mapped)
    public void onMapped(View view) {
        MainActivity.get(view.getContext()).navigateTo(MappedKey.create());
    }

    @OnClick(R.id.button_mapped_rx)
    public void onMappedRx(View view) {
        MainActivity.get(view.getContext()).navigateTo(MappedRxKey.create());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        CustomApplication.getInjector(context).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }
}
