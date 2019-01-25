package com.suhen.android.libble.simple;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.os.Build;

import com.suhen.android.libble.peripheral.BlePeripheral;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SimpleBlePeripheral extends BlePeripheral {
    private static final String TAG = "SimpleBlePeripheral";
    private volatile ExecutorService mExecutorService;

    protected SimpleBlePeripheral(Context context) {
        super(context);
    }

    @Override
    protected void peripheralNotSupport() {
    }

    @Override
    protected void onPeripheralStartSuccess(AdvertiseSettings settingsInEffect) {
    }

    @Override
    protected void onPeripheralStartFailure(int errorCode) {
    }

    @Override
    protected void onConnected(BluetoothDevice device) {
    }

    @Override
    protected void onDisconnected(BluetoothDevice device) {
    }

    @Override
    protected String generatePeripheralName() {
        return "";
    }

    @Override
    protected AdvertiseSettings generateAdvertiseSettings() {
        return new AdvertiseSettings.Builder().build();
    }

    @Override
    protected AdvertiseData generateAdvertiseData() {
        return new AdvertiseData.Builder().build();
    }

    /**
     * Note that this is the simplest implementation
     */
    @Override
    protected void addGattService() {
    }

    protected synchronized ExecutorService getBlockingService() {
        if (mExecutorService == null || mExecutorService.isShutdown() || mExecutorService.isTerminated()) {
            mExecutorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(Runtime.getRuntime().availableProcessors() * 2 + 2), (r, exec) -> {
                try {
                    if (!exec.isShutdown()) {
                        exec.getQueue().put(r);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        return mExecutorService;
    }
}
