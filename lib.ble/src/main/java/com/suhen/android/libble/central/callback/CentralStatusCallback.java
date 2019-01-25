package com.suhen.android.libble.central.callback;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;

import com.suhen.android.libble.central.base.BleBaseCentral;
import com.suhen.android.libble.central.sdk.BleScanRecord;

import java.util.List;
import java.util.UUID;

/**
 * Created by andy
 * 2019/1/16.
 * Email: 1239604859@qq.com
 */
public class CentralStatusCallback extends BleBaseCentral {
    @Override
    public final int scanTimeout() {
        return 0;
    }

    @Override
    public final boolean isFactoryReset() {
        return false;
    }

    @Override
    public final UUID[] scanFilters() {
        return new UUID[0];
    }

    @Override
    public final List<ScanFilter> scanLollipopFilters() {
        return null;
    }

    @Override
    public void onScanStarted() {
    }

    @Override
    public void onScanFinished() {
    }

    @Override
    public void onScannedPeripheral(ScanResult result, BleScanRecord bleScanRecord, BluetoothDevice remoteDevice, int rssi) {
    }

    @Override
    public void onConnectStarted(BluetoothGatt bluetoothGatt) {
    }

    @Override
    public void onConnected(BluetoothGatt bluetoothGatt, int status) {
    }

    @Override
    public void onConnectFailed(BluetoothGatt bluetoothGatt, int status) {
    }

    @Override
    public void onDisconnected(BluetoothGatt bluetoothGatt) {
    }
}
