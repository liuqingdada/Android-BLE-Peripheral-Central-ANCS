package com.suhen.android.libble.peripheral.callback;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by andy
 * 2019/1/24.
 * Email: 1239604859@qq.com
 */
public abstract class BasePeripheralCallback {
    private String mParentUuid;
    private String mChildUuid;

    public BasePeripheralCallback(String parentUuid, String childUuid) {
        mParentUuid = parentUuid;
        mChildUuid = childUuid;
    }

    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
    }

    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
            boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
    }

    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
    }

    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
            boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
    }

    public String getParentUuid() {
        return mParentUuid;
    }

    public String getChildUuid() {
        return mChildUuid;
    }
}
