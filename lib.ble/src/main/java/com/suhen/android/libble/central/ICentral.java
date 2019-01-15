package com.suhen.android.libble.central;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.suhen.android.libble.central.callback.BleBaseCallback;

/**
 * Created by liuqing
 * 2018/7/30.
 * Email: suhen0420@163.com
 */
public interface ICentral {
    String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";
    int CONNECT_ERROR_GATT_UNKNOWN = 0xFFFFFF;

    void onCreate();

    /**
     * init and start scan
     */
    void setup();

    /**
     * if timeout and not found peripheral
     */
    void scan();

    void stopScan();

    void addBleBaseCallback(BleBaseCallback bleBaseCallback);

    BluetoothGatt connect(BluetoothDevice bluetoothDevice, boolean autoConnect, int transport, int phy);

    boolean isConnected();

    IOperator newOperator(String serviceUUID, String characteristicUUID);

    IOperator newOperator(String serviceUUID, String characteristicUUID, String descriptorUUID);

    void onDestroy();

    interface IOperator {
        void enableCharacteristicNotify(boolean userCharacteristicDescriptor);

        void disableCharacteristicNotify(boolean useCharacteristicDescriptor);

        void enableCharacteristicIndicate(boolean useCharacteristicDescriptor);

        void disableCharacteristicIndicate(boolean userCharacteristicDescriptor);

        void writeCharacteristic(byte[] data);

        void readCharacteristic();

        void writeDescriptor(byte[] data);

        void readDescriptor();

        void readRemoteRssi();

        void setMtu(int requiredMtu);

        void requestConnectionPriority(int connectionPriority);
    }
}
