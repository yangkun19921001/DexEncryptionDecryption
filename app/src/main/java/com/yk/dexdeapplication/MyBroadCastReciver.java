package com.yk.dexdeapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class MyBroadCastReciver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("DevYK", "reciver:" + context);
        Log.i("DevYK","reciver:" + context.getApplicationContext());
        Log.i("DevYK","reciver:" + context.getApplicationInfo().className);

    }
}
