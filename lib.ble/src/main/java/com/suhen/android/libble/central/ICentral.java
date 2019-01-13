package com.suhen.android.libble.central;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.suhen.android.libble.central.callback.BleBaseCallback;
import com.suhen.android.libble.central.callback.BleMtuChangedCallback;
import com.suhen.android.libble.central.callback.BleRssiCallback;

/**
 * Created by liuqing
 * 2018/7/30.
 * Email: suhen0420@163.com
 */
public interface ICentral {
    void onCreate();

    boolean isSupportCentral();

    /**
     * init and start scan
     */
    void setup();

    /**
     * if timeout and not found peripheral
     */
    void scan();

    void addBleBaseCallback(BleBaseCallback bleBaseCallback);

    void setBleRssiCallback(BleRssiCallback bleRssiCallback);

    void setBleMtuChangedCallback(BleMtuChangedCallback bleMtuChangedCallback);

    BluetoothGatt connect(BluetoothDevice bluetoothDevice, boolean autoConnect, int transport, int phy);

    void preparePair();

    boolean isConnected();

    void onDestroy();

    void sendBleBytes(byte[] bytes);
}
