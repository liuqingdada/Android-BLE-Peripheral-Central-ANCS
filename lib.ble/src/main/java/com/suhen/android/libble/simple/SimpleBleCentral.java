package com.suhen.android.libble.simple;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.util.Log;

import com.suhen.android.libble.central.BleCentral;

/**
 * Created by suhen
 * 18-8-5.
 * Email: 1239604859@qq.com
 */
public class SimpleBleCentral extends BleCentral {

    protected SimpleBleCentral(Context context) {
        super(context);
    }

    @Override
    protected int scanTimeout() {
        return 0;
    }

    @Override
    protected int reconnectCount() {
        return 0;
    }

    @Override
    protected String scanDeviceName() {
        return null;
    }

    @Override
    protected int manufacturerId() {
        return 0xFFFF;
    }

    @Override
    protected byte[] manufacturerData() {
        return new byte[0];
    }

    @Override
    protected void onScanStarted() {
        Log.d(TAG, "onScanStarted: ");
    }

    @Override
    protected void onScanFinished() {
        Log.d(TAG, "onScanFinished: ");
    }

    @Override
    protected void onScannedPeripheral(BluetoothDevice bluetoothDevice, int rssi, String deviceName,
            byte[] manufacturerData) {
        super.onScannedPeripheral(bluetoothDevice, rssi, deviceName, manufacturerData);
    }

    @Override
    protected void onConnectStarted(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice) {

    }

    @Override
    protected void onConnected() {

    }

    @Override
    protected void onConnectFailed(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice) {

    }
}
