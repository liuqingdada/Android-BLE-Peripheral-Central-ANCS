package com.suhen.android.libble.central;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/**
 * Created by liuqing
 * 2018/7/30.
 * Email: suhen0420@163.com
 */
public interface ICentral {
    void onCreate();

    boolean isSupportCentral();

    void setup();

    BluetoothGatt connect(BluetoothDevice bluetoothDevice,
                          boolean autoConnect,
                          int transport,
                          int phy);

    void preparePair();

    boolean isConnected();

    void onDestroy();

    void sendBleBytes(byte[] bytes);
}
