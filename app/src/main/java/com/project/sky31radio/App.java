package com.project.sky31radio;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.project.sky31radio.module.AppModule;
import com.project.sky31radio.module.Injector;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import cn.jpush.android.api.JPushInterface;
import dagger.ObjectGraph;
import timber.log.Timber;

/**
 * Created by linroid on 1/14/15.
 */

public class App extends Application
        implements Injector, SharedPreferences.OnSharedPreferenceChangeListener {
    ObjectGraph mObjectGraph;

    @Inject
    Timber.Tree tree;
    public void onCreate() {
        super.onCreate();
        JPushInterface.setDebugMode(true);
        JPushInterface.init(this);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        mObjectGraph = ObjectGraph.create(getModules().toArray());
        inject(this);
        Timber.plant(tree);
        initLeanCloud();
    }

    private void initLeanCloud() {
//        AVOSCloud.initialize(this, BuildConfig.LEANCLOUD_APP_ID,BuildConfig.LEANCLOUD_APP_KEY);
//        AVInstallation.getCurrentInstallation().saveInBackground();
//        PushService.setDefaultPushCallback(this, HomeActivity.class);
//        AVAnalytics.enableCrashReport(this, true);
        setNotification(PreferenceManager.getDefaultSharedPreferences(this));
    }

    public List<Object> getModules() {
        return Arrays.<Object>asList(new AppModule(this));
    }

    public void inject(Object target) {
        this.mObjectGraph.inject(target);
    }

    public ObjectGraph plus(Injector injector) {
        return this.mObjectGraph.plus(injector.getModules().toArray());
    }

    public ObjectGraph plus(Object[] modules) {
        return this.mObjectGraph.plus(modules);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getString(R.string.pref_allow_new_program_notification))){
            setNotification(sharedPreferences);
        }
    }
    private void setNotification(SharedPreferences sharedPreferences){
        boolean enableNotification = sharedPreferences.getBoolean(getString(R.string.pref_allow_new_program_notification), true);
        Timber.d("setNotification:%s",String.valueOf(enableNotification));
//        if(enableNotification){
//            PushService.subscribe(this, Constants.CHANNEL_NEW_PROGRAM, HomeActivity.class);
//        }else{
//            PushService.unsubscribe(this, Constants.CHANNEL_NEW_PROGRAM);
//        }
    }
}