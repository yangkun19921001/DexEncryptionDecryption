package com.yk.dexdeapplication;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

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

        DateUtils dateUtils = new DateUtils();
        dateUtils.setCurrentDate(new Date());
        Toast.makeText(getApplicationContext(), dateUtils.getCurrentDate(), Toast.LENGTH_SHORT).show();

    }
}
