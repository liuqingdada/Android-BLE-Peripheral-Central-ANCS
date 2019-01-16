package com.suhen.android.libble.central.base;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.suhen.android.libble.central.ICentral;
import com.suhen.android.libble.central.sdk.BleScanRecord;

import java.util.List;
import java.util.UUID;

/**
 * Created by andy
 * 2019/1/14.
 * Email: 1239604859@qq.com
 */
public abstract class BleBaseCentral {
    protected static final int DEFAULT_SCAN_TIMEOUT = 60;
    protected static final int TRANSPORT_AUTO = 0;
    protected static final int PHY_LE_1M_MASK = 1;

    /**
     * @return default timeout is 60s
     * unit is {@link java.util.concurrent.TimeUnit#SECONDS}
     */
    protected abstract int scanTimeout();

    protected abstract boolean isFactoryReset();

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected abstract UUID[] scanFilters();

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    protected abstract List<ScanFilter> scanLollipopFilters();

    protected abstract void onScanStarted();

    protected abstract void onScanFinished();

    /**
     * @param result        for TargetApi({@link Build.VERSION_CODES#LOLLIPOP})
     * @param bleScanRecord for TargetApi({@link Build.VERSION_CODES#JELLY_BEAN_MR2})
     * @param remoteDevice  for TargetApi({@link Build.VERSION_CODES#JELLY_BEAN_MR2})
     * @param rssi          for TargetApi({@link Build.VERSION_CODES#JELLY_BEAN_MR2})
     */
    protected abstract void onScannedPeripheral(ScanResult result, BleScanRecord bleScanRecord, BluetoothDevice remoteDevice, int rssi);

    protected abstract void onConnectStarted(BluetoothGatt bluetoothGatt);

    protected abstract void onConnected(BluetoothGatt bluetoothGatt, int status);

    protected abstract void onConnectFailed(BluetoothGatt bluetoothGatt, int status);

    protected abstract void onDisconnected(BluetoothGatt bluetoothGatt);

    protected abstract class AbstractOperator implements ICentral.IOperator {
    }
}
