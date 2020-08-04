package com.my.demo;

import android.app.Application;

import com.wosmart.ukprotocollibary.WristbandManager;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        WristbandManager.getInstance(this);
    }
}
