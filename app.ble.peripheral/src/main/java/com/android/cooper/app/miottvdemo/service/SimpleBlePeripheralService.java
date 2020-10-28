package com.android.cooper.app.miottvdemo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.core.app.NotificationManagerCompat;

import com.android.common.utils.LogUtil;
import com.android.cooper.app.miottvdemo.R;
import com.suhen.android.libble.BLE;
import com.suhen.android.libble.peripheral.IPeripheral;
import com.suhen.android.libble.permission.NotificationWizard;

public class SimpleBlePeripheralService extends Service {
    private static final String TAG = "SimpleBlePeripheralServ";
    private static final int FOREGROUND_ID = 0xFFFF;

    private PeripheralBinder mPeripheralBinder;
    private IPeripheral mPeripheral;

    public SimpleBlePeripheralService() {
    }

    public class PeripheralBinder extends Binder {
        public SimpleBlePeripheralService getService() {
            return SimpleBlePeripheralService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mPeripheralBinder == null) {
            mPeripheralBinder = new PeripheralBinder();
        }
        return mPeripheralBinder;
    }

    @Override
    public void onCreate() {
        LogUtil.d(TAG, "onCreate: ");
        super.onCreate();
        startForegroound();

        try {
            mPeripheral = BLE.newPeripheral(SimpleBlePeripheralImpl.class);
            mPeripheral.onCreate();
            mPeripheral.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d(TAG, "onStartCommand: ");
        return START_STICKY;
    }

    private void startForegroound() {
        LogUtil.d(TAG, "startForeground: ");

        startForeground(FOREGROUND_ID, NotificationWizard.generateNotification(
                this, NotificationManagerCompat.IMPORTANCE_DEFAULT, R.mipmap.ic_launcher,
                getClass(), 1
        ));
    }

    @Override
    public void onDestroy() {
        LogUtil.d(TAG, "onDestroy: ");
        super.onDestroy();
        mPeripheral.onDestroy();
    }

    public IPeripheral getPeripheral() {
        return mPeripheral;
    }
}
