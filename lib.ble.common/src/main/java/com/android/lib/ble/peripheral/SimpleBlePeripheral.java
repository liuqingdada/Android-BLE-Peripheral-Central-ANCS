package com.android.lib.ble.peripheral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.os.Build;

import androidx.annotation.RequiresApi;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class SimpleBlePeripheral extends BlePeripheral {
    protected static final String TAG = "BlePeripheral";

    protected SimpleBlePeripheral() {
    }

    @Override
    protected void peripheralNotSupport() {
    }

    @Override
    protected void onOpenGattServerError(BluetoothAdapter adapter) {
    }

    @Override
    protected void onPeripheralStartSuccess(AdvertiseSettings settingsInEffect) {
    }

    @Override
    protected void onPeripheralStartFailure(int errorCode) {
    }

    @Override
    protected void onConnected(BluetoothDevice device) {
    }

    @Override
    protected void onDisconnected(BluetoothDevice device) {
    }

    @Override
    protected String generatePeripheralName() {
        return "";
    }

    @Override
    protected AdvertiseSettings generateAdvertiseSettings() {
        return new AdvertiseSettings.Builder().build();
    }

    @Override
    protected AdvertiseData generateAdvertiseData() {
        return new AdvertiseData.Builder().build();
    }

    @Override
    protected AdvertiseData generateAdvertiseResponse() {
        return new AdvertiseData.Builder().build();
    }

    @Override
    protected void addGattService() {
    }
}
