package com.suhen.android.libble.peripheral.base;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;

/**
 * Created by andy
 * 2019/1/24.
 * Email: 1239604859@qq.com
 */
public abstract class BleBasePeripheral {

    protected abstract void peripheralNotSupport();

    protected abstract void onPeripheralStartSuccess(AdvertiseSettings settingsInEffect);

    protected abstract void onPeripheralStartFailure(int errorCode);

    protected abstract void onConnected(BluetoothDevice device);

    protected abstract void onDisconnected(BluetoothDevice device);

    /**
     * Note that some devices do not support long names.
     * Recommended within 12 bytes.
     */
    protected abstract String generatePeripheralName();

    /**
     * @return Your BLE peripheral settings.
     */
    protected abstract AdvertiseSettings generateAdvertiseSettings();

    protected abstract AdvertiseData generateAdvertiseData();

    protected abstract AdvertiseData generateAdvertiseResponse();

    /**
     * All params must be standard UUID, you can use {@link java.util.UUID} generate this param,
     * or manually generated UUID.
     * <p>
     * If you use illegal UUID, peripheral will be open failure.
     */
    protected abstract void addGattService();

}
