package com.suhen.android.appcentral.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import com.suhen.android.libble.central.sdk.BleScanRecord;
import com.suhen.android.libble.simple.SimpleBleCentral;

/**
 * Created by andy
 * 2019/1/15.
 * Email: 1239604859@qq.com
 */
public class BleCentralImpl extends SimpleBleCentral {
    private static final String TAG = "BleCentralImpl";

    protected BleCentralImpl(Context context) {
        super(context);
    }

    @Override
    protected void onScanStarted() {
        super.onScanStarted();
        Log.d(TAG, "onScanStarted: ");
    }

    @Override
    protected void onScanFinished() {
        super.onScanFinished();
        Log.d(TAG, "onScanFinished: ");
    }

    @Override
    protected void onScannedPeripheral(int callbackType, ScanResult result, BleScanRecord bleScanRecord) {
        getBlockingService().execute(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && result != null) {
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord != null) {

                    BluetoothDevice remoteDevice = result.getDevice();
                    String deviceName = scanRecord.getDeviceName();
                    SparseArray<byte[]> manufacturerSpecificData = scanRecord.getManufacturerSpecificData();

                    if ("OTA_FFFFFFFFFFFF".equals(deviceName) || "OTA_FFFFFFFFFFFF".equals(remoteDevice.getName())) {
                        Log.d(TAG, "onScannedPeripheral: " + result);

                        stopScan();

                        connect(remoteDevice, false, TRANSPORT_AUTO, PHY_LE_1M_MASK);
                    }
                }

            } else if (bleScanRecord != null) {
                Log.d(TAG, "onScannedPeripheral: " + bleScanRecord.toString());

            }
        });
    }

    @Override
    protected void onConnectStarted(BluetoothGatt bluetoothGatt) {
        super.onConnectStarted(bluetoothGatt);
        Log.d(TAG, "onConnectStarted: ");
    }

    @Override
    protected void onConnected(BluetoothGatt bluetoothGatt, int status) {
        super.onConnected(bluetoothGatt, status);
        Log.d(TAG, "onConnected: ");
    }

    @Override
    protected void onConnectFailed(BluetoothGatt bluetoothGatt, int status, boolean isGattCallback) {
        super.onConnectFailed(bluetoothGatt, status, isGattCallback);
        Log.d(TAG, "onConnectFailed: ");
    }

    @Override
    protected void onDisconnected(BluetoothGatt bluetoothGatt) {
        super.onDisconnected(bluetoothGatt);
        Log.d(TAG, "onDisconnected: ");
    }
}
