package com.yk.dexdeapplication;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("DevYK", "MyApplication onCreate()");
        Toast.makeText(getApplicationContext(), "Application 替换成功", Toast.LENGTH_LONG).show();
    }
}
