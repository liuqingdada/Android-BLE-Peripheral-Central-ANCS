package com.android.cooper.app.miottvdemo.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.os.ParcelUuid;

import com.android.common.utils.LogUtil;
import com.android.lib.ble.peripheral.SimpleBlePeripheral;
import com.android.lib.ble.utils.ByteUtil;

import java.util.UUID;

/**
 * Created by andy
 * 2019/1/25.
 * Email: 1239604859@qq.com
 */
public class SimpleBlePeripheralImpl extends SimpleBlePeripheral {
    private static final String TAG = "BlePeripheralImpl";
    private static final ParcelUuid MI_SERVICE_UUID =
            ParcelUuid.fromString("0000FE95-0000-1000-8000-00805F9B34FB");
    // UUID
    public static final String SERVICE_UUID = "4622c045-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_INDICATE_UUID = "4622c046-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_WRITE_UUID = "4622c047-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_READ_UUID = "4622c048-1cd2-4211-adc5-89df72c789ec";

    private static final int COMPANY_ID = 0x038F;

    protected SimpleBlePeripheralImpl() {
    }

    @Override
    protected void peripheralNotSupport() {
        LogUtil.d(TAG, "peripheralNotSupport: ");
    }

    @Override
    protected void onPeripheralStartSuccess(AdvertiseSettings settingsInEffect) {
        LogUtil.d(TAG, "onPeripheralStartSuccess: ");
    }

    @Override
    protected void onPeripheralStartFailure(int errorCode) {
        LogUtil.d(TAG, "onPeripheralStartFailure: ");
        stopAdvertising();
    }

    @Override
    protected void onConnected(BluetoothDevice device) {
        LogUtil.d(TAG, "onConnected: ");
    }

    @Override
    protected void onDisconnected(BluetoothDevice device) {
        LogUtil.d(TAG, "onDisconnected: ");
        stopAdvertising();
        startAdvertising();
    }

    @Override
    protected String generatePeripheralName() {
        /*String mac = StringUtil.getString(mContext, BLE.PERIPHERAL_MAC, "");
        mac = mac.substring(mac.length() - 7);
        mac = mac.replaceAll(":", "");*/
        return "Suhen";
    }

    @Override
    protected AdvertiseSettings generateAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTimeout(0)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();
    }

    @Override
    protected AdvertiseData generateAdvertiseData() {
        byte[] frameControl = {0x00, 0x00};
        byte[] productId = {0x00, 0x01};
        byte[] frameCounter = {0x02};
        return new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceData(
                        MI_SERVICE_UUID,
                        ByteUtil.combineBytes(
                                frameControl,
                                productId,
                                frameCounter
                        )
                )
                .build();
    }

    @Override
    protected AdvertiseData generateAdvertiseResponse() {
        byte[] frameControl = {0x00, 0x00};
        byte[] productId = {0x00, 0x01};
        byte[] frameCounter = {0x02};
        byte[] btAdress = {0x00, 0x00, 0x46, 0x66, 0x30, 0x01};
        byte[] capability = {0x03};
        byte[] ioCapability = {0x04};
        byte[] object = {0x05};/*
        byte[] randomNum = {0x06, 0x07, 0x08};
        byte[] mic = {0x09, 0x10, 0x11, 0x12};*/

        return new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addManufacturerData(
                        COMPANY_ID,
                        ByteUtil.combineBytes(
                                frameControl,
                                productId,
                                frameCounter,
                                btAdress,
                                capability,
                                ioCapability,
                                object/*,
                                randomNum,
                                mic*/
                        )
                )
                .build();
    }

    /**
     * Note that this is the simplest implementation
     */
    @Override
    protected void addGattService() {
        BluetoothGattService peripheralService = new BluetoothGattService(
                UUID.fromString(SERVICE_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY
        );

        BluetoothGattCharacteristic characteristicIndicate = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_INDICATE_UUID),
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_WRITE_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        peripheralService.addCharacteristic(characteristicWrite);
        peripheralService.addCharacteristic(characteristicIndicate);

        mBluetoothGattServer.addService(peripheralService);
    }
}
