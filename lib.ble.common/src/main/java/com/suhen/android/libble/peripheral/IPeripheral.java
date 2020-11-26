package com.suhen.android.libble.peripheral;

import android.bluetooth.BluetoothGattCharacteristic;

import com.suhen.android.libble.message.BleMessage;
import com.suhen.android.libble.peripheral.callback.BasePeripheralCallback;
import com.suhen.android.libble.peripheral.callback.BluetoothCallback;

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

    void cancelConnection();

    void notify(String serviceUUID, String characterUUID, byte[] data);

    void notify(BluetoothGattCharacteristic characteristic, byte[] data);

    void indicate(String serviceUUID, String characterUUID, BleMessage message);

    void indicate(BluetoothGattCharacteristic characteristic, BleMessage message);

    void onDestroy();

    void setBluetoothCallback(BluetoothCallback bluetoothCallback);
}
