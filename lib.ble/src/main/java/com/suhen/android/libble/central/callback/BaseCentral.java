package com.suhen.android.libble.central.callback;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.os.Build;

import com.suhen.android.libble.central.sdk.BleScanRecord;

import java.util.List;

/**
 * Created by andy
 * 2019/1/14.
 * Email: 1239604859@qq.com
 */
public abstract class BaseCentral {
    protected static final int DEFAULT_SCAN_TIMEOUT = 60;

    /**
     * default timeout is 60s
     */
    protected abstract int scanTimeout();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected abstract List<ScanFilter> scanFilters();

    protected abstract void onScanStarted();

    protected abstract void onScanFinished();

    /**
     * @param callbackType  TargetApi({@link Build.VERSION_CODES#LOLLIPOP})
     * @param result        TargetApi({@link Build.VERSION_CODES#LOLLIPOP})
     * @param bleScanRecord TargetApi({@link Build.VERSION_CODES#JELLY_BEAN_MR2})
     */
    protected abstract void onScannedPeripheral(int callbackType, ScanResult result, BleScanRecord bleScanRecord);

    protected abstract void onConnectStarted(BluetoothGatt bluetoothGatt);

    protected abstract void onConnected(BluetoothGatt bluetoothGatt, int status);

    protected abstract void onConnectFailed(BluetoothGatt bluetoothGatt, int status, boolean isGattCallback);

    protected abstract void onDisconnected(BluetoothGatt bluetoothGatt);
}
