package com.suhen.android.libble.peripheral;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import com.suhen.android.libble.peripheral.callback.BasePeripheralCallback;

/**
 * Created by liuqing
 * 2018/7/30.
 * Email: suhen0420@163.com
 */
public interface IPeripheral {
    void onCreate();

    void addBasePeripheralCallback(BasePeripheralCallback basePeripheralCallback);

    void setup();

    void preparePair();

    boolean isConnected();

    void sendResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value);

    void notify(BluetoothGattCharacteristic characteristic);

    //////
    boolean lockCurrentIndication(BluetoothGattCharacteristic indicationCharacteristic);

    void unlockCurrentIndication();

    boolean indicate(byte[] value);
    //////

    void onDestroy();
}
