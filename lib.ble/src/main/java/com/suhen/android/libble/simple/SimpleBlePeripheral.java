package com.suhen.android.libble.simple;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.peripheral.BlePeripheral;
import com.suhen.android.libble.utils.StringUtil;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SimpleBlePeripheral extends BlePeripheral {

    private static final int MANUFACTURER_ID = 0xFFFF;

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
    protected void onConnected(BluetoothDevice device) {
        // verify central device

        // send a blemessage, if didn't get the desired response data to disconnect


    }

    @Override
    protected void onDisconnected(BluetoothDevice device) {
        stopAdvertising();
        startAdvertising();
    }

    @Override
    protected void onReceiveBytes(byte[] bytes) {

    }

    @Override
    public void sendBleBytes(byte[] bytes) {

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

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        Log.v(TAG, "onServiceAdded: status: " + status + ", service: " + service.getUuid());
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            Log.v(TAG, "onServiceAdded: service has characteristic: " + characteristic.getUuid());
        }
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
        if (characteristic.getUuid()
                          .toString()
                          .equals(BLE.CHAR_WRITE_UUID)) {
            // receive data:
            Log.i(TAG, "onCharacteristicWriteRequest: " + Arrays.toString(value));
            onReceiveBytes(value);
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                                              value);
        }
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        Log.i(TAG, "onMtuChanged: mtu = " + mtu);
    }
}
