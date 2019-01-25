package com.suhen.android.appperipheral;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.suhen.android.appperipheral.service.SimpleBlePeripheralService;

public class MainActivity extends AppCompatActivity {

    private SimpleBlePeripheralService mPeripheralService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, SimpleBlePeripheralService.class);
        ContextCompat.startForegroundService(this, intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        findViewById(R.id.button_indicate).setOnClickListener(v -> {
            mPeripheralService.getPeripheral().indicate();
        });
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SimpleBlePeripheralService.PeripheralBinder binder = (SimpleBlePeripheralService.PeripheralBinder) service;
            mPeripheralService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPeripheralService = null;
        }
    };
}
