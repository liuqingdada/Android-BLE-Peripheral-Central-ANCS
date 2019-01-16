package com.suhen.android.appcentral;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.suhen.android.appcentral.service.SimpleBleCentralService;
import com.suhen.android.libble.BLE;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SimpleBleCentralService mBleCentralService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BLE.isSupportBle(this)) {
            Intent intent = new Intent(this, SimpleBleCentralService.class);
            ContextCompat.startForegroundService(this, intent);
            bindService(intent, mBleCentralServiceConnection, Context.BIND_AUTO_CREATE);
        }
        findViewById(R.id.bt_rescan).setOnClickListener(v -> {

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private ServiceConnection mBleCentralServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBleCentralService = (SimpleBleCentralService) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleCentralService = null;
        }
    };
}
