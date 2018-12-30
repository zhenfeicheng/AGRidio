package com.project.sky31radio.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.reflect.TypeToken;
import com.project.sky31radio.R;
import com.project.sky31radio.data.ApiService;
import com.project.sky31radio.data.DiskCacheManager;
import com.project.sky31radio.model.Anchor;
import com.project.sky31radio.model.Pagination;
import com.project.sky31radio.ui.adapter.AnchorAdapter;
import com.project.sky31radio.ui.base.InjectableFragment;
import com.project.sky31radio.view.ContentLoaderView;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import timber.log.Timber;


public class AnchorListFragment extends InjectableFragment implements ContentLoaderView.OnRefreshListener {

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public static final String KEY_USER = "key_user";

    @InjectView(R.id.content_loader)
    ContentLoaderView loaderView;
    @InjectView(R.id.recycler)
    RecyclerView recyclerView;

    @Inject
    ApiService apiService;
    @Inject
    Picasso picasso;
    @Inject
    DiskCacheManager cacheManager;

    boolean hasLoaded = false;
    AnchorAdapter adapter;
    AnchorAdapter.OnAnchorSelectedListener listener;

    public static AnchorListFragment newInstance() {
        AnchorListFragment fragment = new AnchorListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public AnchorListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new AnchorAdapter(getActivity(), picasso);
        adapter.setOnAnchorSelectedListener(listener);
        if(savedInstanceState!=null){
            List<Anchor> anchorList = savedInstanceState.getParcelableArrayList(KEY_USER);
            adapter.setListData(anchorList);
        }else{
            loadData(1);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_USER, (ArrayList<Anchor>) adapter.getAnchorList());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_anchor_list, container, false);
        ButterKnife.inject(this, view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        loaderView.setAdapter(adapter);
        loaderView.setOnRefreshListener(this);

        return  view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof AnchorAdapter.OnAnchorSelectedListener){
            listener = (AnchorAdapter.OnAnchorSelectedListener) activity;
        }else{
            throw new IllegalArgumentException("activity must implement AnchorAdapter.OnAnchorSelectedListener");
        }
    }

    public void loadData(final int page){
        if(!hasLoaded){
            AppObservable.bindFragment(this,
                    Observable.create(new Observable.OnSubscribe<Pagination<Anchor>>() {
                    @Override
                    public void call(Subscriber<? super Pagination<Anchor>> subscriber) {
                        if (cacheManager.exits(DiskCacheManager.KEY_ANCHOR)) {
                            Type type = new TypeToken<Pagination<Anchor>>() { }.getType();
                            Pagination<Anchor> cachedData = cacheManager.get(DiskCacheManager.KEY_ANCHOR, type);
                            Timber.d("load data from cached file successful!");
                            if(cachedData!=null) {
                                subscriber.onNext(cachedData);
                            }
                        }
                    }
                })
            )
            .subscribe(observer);
        }
        AppObservable.bindFragment(this, apiService.listAnchor(page))
                .map(new Func1<Pagination<Anchor>, Pagination<Anchor>>() {
                    @Override
                    public Pagination<Anchor> call(Pagination<Anchor> pagination) {
                        cacheManager.put(DiskCacheManager.KEY_ANCHOR, pagination);
                        return pagination;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }
    Observer<Pagination<Anchor>> observer = new Observer<Pagination<Anchor>>() {
        @Override
        public void onCompleted() {
            Timber.i("listAnchor onCompleted");
        }

        @Override
        public void onError(Throwable throwable) {
            Timber.e(throwable, "发生错误: %s", throwable.getMessage());
            loaderView.notifyLoadFailed(throwable);
        }

        @Override
        public void onNext(Pagination<Anchor> pagination) {
            Timber.d("onNext %s", pagination.toString());
            hasLoaded = true;
            loaderView.setPage(pagination.getCurrentPage(), pagination.getLastPage());
            adapter.setListData(pagination.getData());
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onRefresh(boolean fromSwipe) {
        loadData(1);
    }
}
