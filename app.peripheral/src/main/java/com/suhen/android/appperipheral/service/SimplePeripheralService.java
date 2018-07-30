package com.suhen.android.appperipheral.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.peripheral.IPeripheral;
import com.suhen.android.libble.simple.SimpleBlePeripheral;

public class SimplePeripheralService extends Service {

    private IPeripheral mPeripheral;

    public SimplePeripheralService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            mPeripheral = BLE.peripheral(SimpleBlePeripheral.class, this);
            mPeripheral.onCreate();
            if (mPeripheral.isSupportBle()) {
                mPeripheral.setup();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPeripheral.onDestroy();
    }
}
