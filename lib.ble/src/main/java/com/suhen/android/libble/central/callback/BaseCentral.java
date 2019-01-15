package com.suhen.android.libble.central.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.suhen.android.libble.central.sdk.BleScanRecord;

import java.util.List;
import java.util.UUID;

/**
 * Created by andy
 * 2019/1/14.
 * Email: 1239604859@qq.com
 */
public abstract class BaseCentral {
    protected static final int DEFAULT_SCAN_TIMEOUT = 60;
    protected static final int DEFAULT_RECONNECT_COUNT = 0;

    /**
     * @return default timeout is 60s
     * unit is {@link java.util.concurrent.TimeUnit#SECONDS}
     */
    protected abstract int scanTimeout();

    /**
     * @return default reconnect count is 0
     */
    protected abstract int tryReconnectCount();

    protected abstract boolean isFactoryReset();

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected abstract UUID[] scanFilters();

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    protected abstract List<ScanFilter> scanLollipopFilters();

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
