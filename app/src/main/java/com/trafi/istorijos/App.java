package com.trafi.istorijos;

import android.app.Application;

import com.mapbox.mapboxsdk.MapboxAccountManager;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MapboxAccountManager.start(this, BuildConfig.MAPBOX_ACCESS_TOKEN);
    }

}
