package com.yk.dexdeapplication;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Log.i("DevYK", "activity:" + getApplication());
        Log.i("DevYK", "activity:" + getApplicationContext());
        Log.i("DevYK", "activity:" + getApplicationInfo().className);

        startService(new Intent(this, MyService.class));

        Intent intent = new Intent("com.yk.dexdeapplication_devyk");
        intent.setComponent(new ComponentName(getPackageName(), MyBroadCastReciver.class.getName
                ()));

        sendBroadcast(intent);

        getContentResolver().delete(Uri.parse("content://com.yk.dexdeapplication.MyProvider"), null,
                null);


        byte[] src = new byte[]{1,2,3,4};
        byte[] dest = new byte[10];

        System.arraycopy(src,0,dest,0,src.length);
        Log.d("","");

    }
}
