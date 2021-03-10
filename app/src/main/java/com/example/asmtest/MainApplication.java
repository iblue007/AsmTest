package com.example.asmtest;

import android.app.Application;

import io.github.iamyours.router.ARouter;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ARouter.getInstance().init();
    }
}
