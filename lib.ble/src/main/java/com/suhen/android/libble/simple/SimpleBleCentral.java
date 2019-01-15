package com.suhen.android.libble.simple;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.suhen.android.libble.central.BleCentral;
import com.suhen.android.libble.central.sdk.BleScanRecord;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by suhen
 * 18-8-5.
 * Email: 1239604859@qq.com
 */
public class SimpleBleCentral extends BleCentral {
    private volatile ExecutorService mExecutorService;

    protected SimpleBleCentral(Context context) {
        super(context);
        mExecutorService = getBlockingService();
    }

    @Override
    protected int scanTimeout() {
        return DEFAULT_SCAN_TIMEOUT;
    }

    @Override
    protected int tryReconnectCount() {
        return DEFAULT_RECONNECT_COUNT;
    }

    @Override
    protected boolean isFactoryReset() {
        return false;
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected UUID[] scanFilters() {
        return null;
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    protected List<ScanFilter> scanLollipopFilters() {
        return null;
    }

    @Override
    protected void onScanStarted() {
    }

    @Override
    protected void onScanFinished() {
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

    protected synchronized ExecutorService getBlockingService() {
        if (mExecutorService == null || mExecutorService.isShutdown() || mExecutorService.isTerminated()) {
            mExecutorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(Runtime.getRuntime().availableProcessors() * 2 + 2), (r, exe) -> {
                try {
                    if (!exe.isShutdown()) {
                        exe.getQueue().put(r);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        return mExecutorService;
    }
}
