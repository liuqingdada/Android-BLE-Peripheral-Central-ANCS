package com.suhen.android.appperipheral.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.util.Log;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.simple.SimpleBlePeripheral;
import com.suhen.android.libble.utils.StringUtil;

import java.util.UUID;

/**
 * Created by andy
 * 2019/1/25.
 * Email: 1239604859@qq.com
 */
public class BlePeripheralImpl extends SimpleBlePeripheral {
    private static final String TAG = "BlePeripheralImpl";
    // UUID
    public static final String SERVICE_UUID = "4622c045-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_INDICATE_UUID = "4622c046-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_WRITE_UUID = "4622c047-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_READ_UUID = "4622c048-1cd2-4211-adc5-89df72c789ec";

    private static final int MANUFACTURER_ID = 0xFFFF;

    protected BlePeripheralImpl(Context context) {
        super(context);
    }

    @Override
    protected void peripheralNotSupport() {
        getBlockingService().execute(() -> {
            Log.d(TAG, "peripheralNotSupport: ");
        });
    }

    @Override
    protected void onPeripheralStartSuccess(AdvertiseSettings settingsInEffect) {
        getBlockingService().execute(() -> {
            Log.d(TAG, "onPeripheralStartSuccess: ");
        });
    }

    @Override
    protected void onPeripheralStartFailure(int errorCode) {
        getBlockingService().execute(() -> {
            Log.d(TAG, "onPeripheralStartFailure: ");
            stopAdvertising();
        });
    }

    @Override
    protected void onConnected(BluetoothDevice device) {
        getBlockingService().execute(() -> {
            Log.d(TAG, "onConnected: ");
        });
    }

    @Override
    protected void onDisconnected(BluetoothDevice device) {
        getBlockingService().execute(() -> {
            Log.d(TAG, "onDisconnected: ");
            stopAdvertising();
            startAdvertising();
        });
    }

    @Override
    protected String generatePeripheralName() {
        String mac = StringUtil.getString(mContext, BLE.PERIPHERAL_MAC, "");
        mac = mac.substring(mac.length() - 7, mac.length());
        mac = mac.replaceAll(":", "");
        return "Suhen_" + mac;
    }

    @Override
    protected AdvertiseSettings generateAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTimeout(0)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();
    }

    @Override
    protected AdvertiseData generateAdvertiseData() {
        byte[] bytes = generatePeripheralName().getBytes();

        return new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addManufacturerData(MANUFACTURER_ID, bytes)
                //.addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID)))
                .build();
    }

    /**
     * Note that this is the simplest implementation
     */
    @Override
    protected void addGattService() {
        getBlockingService().execute(() -> {
            BluetoothGattService peripheralService = new BluetoothGattService(
                    UUID.fromString(SERVICE_UUID),
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);

            BluetoothGattCharacteristic characteristicIndicate = new BluetoothGattCharacteristic(
                    UUID.fromString(CHAR_INDICATE_UUID),
                    BluetoothGattCharacteristic.PROPERTY_INDICATE,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(
                    UUID.fromString(CHAR_WRITE_UUID),
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);

            peripheralService.addCharacteristic(characteristicWrite);
            peripheralService.addCharacteristic(characteristicIndicate);

            mBluetoothGattServer.addService(peripheralService);
        });
    }
}
