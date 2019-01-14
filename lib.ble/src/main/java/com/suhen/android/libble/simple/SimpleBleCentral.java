package com.suhen.android.libble.simple;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import com.suhen.android.libble.central.BleCentral;
import com.suhen.android.libble.central.sdk.BleScanRecord;

import java.util.List;

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
    protected List<ScanFilter> scanFilters() {
        return null;
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
    protected void onScannedPeripheral(int callbackType, ScanResult result, BleScanRecord bleScanRecord) {
    }

    @Override
    protected void onConnectStarted(BluetoothGatt bluetoothGatt) {
    }

    @Override
    protected void onConnected(BluetoothGatt bluetoothGatt, int status) {
    }

    @Override
    protected void onConnectFailed(BluetoothGatt bluetoothGatt, int status, boolean isGattCallback) {
    }

    @Override
    protected void onDisconnected(BluetoothGatt bluetoothGatt) {
    }
}
