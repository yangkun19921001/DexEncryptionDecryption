package com.yk.dexdeapplication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;



public class MyService extends Service {


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("DevYK", "service:" + getApplication());
        Log.i("DevYK", "service:" + getApplicationContext());
        Log.i("DevYK", "service:" + getApplicationInfo().className);
    }
}
