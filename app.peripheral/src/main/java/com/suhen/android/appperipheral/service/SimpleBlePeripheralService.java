package com.suhen.android.appperipheral.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.suhen.android.appperipheral.R;
import com.suhen.android.libble.BLE;
import com.suhen.android.libble.peripheral.IPeripheral;
import com.suhen.android.libble.simple.NotificationWizard;
import com.suhen.android.libble.simple.SimpleBlePeripheral;

public class SimpleBlePeripheralService extends Service {
    private static final String TAG = SimpleBlePeripheralService.class.getSimpleName();
    private static final int FOREGROUND_ID = 0xFFFF;

    private IPeripheral mPeripheral;

    public SimpleBlePeripheralService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        startForegroound();

        try {
            mPeripheral = BLE.newPeripheral(SimpleBlePeripheral.class, this);
            mPeripheral.onCreate();
            mPeripheral.setup();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        return START_STICKY;
    }

    private void startForegroound() {
        Log.d(TAG, "startForeground: ");

        startForeground(FOREGROUND_ID, NotificationWizard.generateNotification(
                this, NotificationManagerCompat.IMPORTANCE_DEFAULT, R.mipmap.ic_launcher,
                getClass(), 1));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        mPeripheral.onDestroy();
    }
}
