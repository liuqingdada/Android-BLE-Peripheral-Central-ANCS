package com.suhen.android.libble.simple;

import android.annotation.TargetApi;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.os.Build;

import com.suhen.android.libble.peripheral.BlePeripheral;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SimpleBlePeripheral extends BlePeripheral {

    private SimpleBlePeripheral(Context context) {
        super(context);
    }

    @Override
    protected void onPeripheralStartSuccess(AdvertiseSettings settingsInEffect) {
    }

    @Override
    protected void onPeripheralStartFailure(int errorCode) {
        stopAdvertising();
    }

    @Override
    protected void onConnected() {
    }

    @Override
    protected void onDisconnected() {
        stopAdvertising();
        startAdvertising();
    }

    @Override
    protected void onReceiveBytes(byte[] bytes) {
    }

    @Override
    public void sendBleBytes(byte[] bytes) {
    }

    /**
     * add more service
     */
    @Override
    protected void addGattService() {
        super.addGattService();
    }
}
