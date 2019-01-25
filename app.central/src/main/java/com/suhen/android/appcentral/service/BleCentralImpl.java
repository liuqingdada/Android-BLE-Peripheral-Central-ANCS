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

import com.suhen.android.libble.central.sdk.BleScanRecord;
import com.suhen.android.libble.simple.SimpleBleCentral;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by andy
 * 2019/1/15.
 * Email: 1239604859@qq.com
 * <p>
 * 自定义，完善自己的协议
 * {@link SimpleBleCentral#getBlockingService()} 用作业务逻辑处理线程，可根据需要另外实现
 */
@SuppressWarnings("FieldCanBeLocal")
public class BleCentralImpl extends SimpleBleCentral {
    private static final String TAG = "BleCentralImpl";

    private Set<BluetoothDevice> mRemoteDevices = new HashSet<>();

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
        Log.d(TAG, "onScanFinished: " + mRemoteDevices);
        mRemoteDevices.clear();
    }

    @Override
    protected void onScannedPeripheral(ScanResult result, BleScanRecord bleScanRecord, BluetoothDevice remoteDevice, int rssi) {
        super.onScannedPeripheral(result, bleScanRecord, remoteDevice, rssi);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && result != null) {
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord != null) {
                if (mRemoteDevices.add(remoteDevice)) {
                    Log.d(TAG, "onScannedPeripheral: " + result);
                }
            }

        } else if (bleScanRecord != null) {
            Log.d(TAG, "onScannedPeripheral: " + bleScanRecord.toString());
            if (mRemoteDevices.add(remoteDevice)) {
            }
        }
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
        for (BluetoothGattService service : bluetoothGatt.getServices()) {
            Log.i(TAG, "onConnected: " + service.getUuid());
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.d(TAG, "onConnected: " + characteristic.getUuid() + ", prop: " + getCharacteristicProperty(characteristic));
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    Log.v(TAG, "onConnected: " + descriptor.getUuid());
                }
            }
        }
    }

    @Override
    protected void onConnectFailed(BluetoothGatt bluetoothGatt, int status) {
        super.onConnectFailed(bluetoothGatt, status);
        Log.d(TAG, "onConnectFailed: ");
    }

    @Override
    protected void onDisconnected(BluetoothGatt bluetoothGatt) {
        super.onDisconnected(bluetoothGatt);
        Log.d(TAG, "onDisconnected: ");
    }
}
