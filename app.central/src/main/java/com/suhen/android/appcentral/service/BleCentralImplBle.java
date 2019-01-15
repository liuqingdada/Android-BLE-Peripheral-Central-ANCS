package com.suhen.android.appcentral.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import com.suhen.android.libble.central.sdk.BleScanRecord;
import com.suhen.android.libble.simple.SimpleBleCentralBle;

/**
 * Created by andy
 * 2019/1/15.
 * Email: 1239604859@qq.com
 */
public class BleCentralImplBle extends SimpleBleCentralBle {
    private static final String TAG = "BleCentralImplBle";

    protected BleCentralImplBle(Context context) {
        super(context);
    }

    @Override
    protected void onScanStarted() {
        getBlockingService().execute(() -> {
            Log.d(TAG, "onScanStarted: ");
        });
    }

    @Override
    protected void onScanFinished() {
        getBlockingService().execute(() -> {
            Log.d(TAG, "onScanFinished: ");
        });
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
        getBlockingService().execute(() -> {
            Log.d(TAG, "onConnectStarted: ");
        });
    }

    @Override
    protected void onConnected(BluetoothGatt bluetoothGatt, int status) {
        getBlockingService().execute(() -> {
            Log.d(TAG, "onConnected: ");
            for (BluetoothGattService service : bluetoothGatt.getServices()) {
                Log.i(TAG, "onConnected: " + service.getUuid());
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    Log.d(TAG, "onConnected: " + characteristic.getUuid());
                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                        Log.v(TAG, "onConnected: " + descriptor.getUuid());
                    }
                }
            }
        });
    }

    @Override
    protected void onConnectFailed(BluetoothGatt bluetoothGatt, int status) {
        getBlockingService().execute(() -> {
            Log.d(TAG, "onConnectFailed: ");
        });
    }

    @Override
    protected void onDisconnected(BluetoothGatt bluetoothGatt) {
        getBlockingService().execute(() -> {
            Log.d(TAG, "onDisconnected: ");
        });
    }
}
