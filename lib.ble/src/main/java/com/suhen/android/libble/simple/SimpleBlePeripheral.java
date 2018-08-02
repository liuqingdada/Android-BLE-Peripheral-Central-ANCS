package com.suhen.android.libble.simple;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.os.Build;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.peripheral.BlePeripheral;

import java.util.UUID;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SimpleBlePeripheral extends BlePeripheral {

    private SimpleBlePeripheral(Context context) {
        super(context);
    }

    @Override
    protected void onPeripheralStartSuccess(AdvertiseSettings settingsInEffect) {

    }

    @Override
    protected void onPeripheralStartFailure(int errorCode) {
        stopAdvertising();
    }

    @Override
    protected void onConnected() {

    }

    @Override
    protected void onDisconnected() {
        stopAdvertising();
        startAdvertising();
    }

    @Override
    protected void onReceiveBytes(byte[] bytes) {

    }

    @Override
    public void sendBleBytes(byte[] bytes) {

    }

    /**
     * Note that this is the simplest implementation
     */
    @Override
    protected void addGattService() {
        BluetoothGattService peripheralService = new BluetoothGattService(
                UUID.fromString(BLE.SERVICE_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic characteristicIndicate = new BluetoothGattCharacteristic(
                UUID.fromString(BLE.CHAR_INDICATE_UUID),
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(
                UUID.fromString(BLE.CHAR_WRITE_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        peripheralService.addCharacteristic(characteristicWrite);
        peripheralService.addCharacteristic(characteristicIndicate);

        mBluetoothGattServer.addService(peripheralService);
    }
}
