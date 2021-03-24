package com.android.lib.ble.peripheral.base;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;

import com.android.lib.ble.message.ActiveRunnable;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by cooper
 * 20-10-20.
 * Email: 1239604859@qq.com
 */
public class IndicateRunnable extends ActiveRunnable {
    private final BluetoothGattServer bluetoothGattServer;
    private final BluetoothDevice connectedDevice;
    private final BluetoothGattCharacteristic characteristic;

    private final ConcurrentLinkedQueue<byte[]> packages = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean idle = new AtomicBoolean(true);

    public IndicateRunnable(
            BluetoothGattServer bluetoothGattServer,
            BluetoothDevice connectedDevice,
            BluetoothGattCharacteristic characteristic
    ) {
        this.bluetoothGattServer = bluetoothGattServer;
        this.connectedDevice = connectedDevice;
        this.characteristic = characteristic;
    }

    @Override
    public void run() {
        if (!idle.get()) {
            return;
        }
        if (idle.compareAndSet(true, false)) {
            send();
        }
    }

    public void next() {
        send();

        // 如果此时没有数据了
        // 证明进入 IDLE 状态
        if (packages.isEmpty()) {
            idle.compareAndSet(false, true);
        }
    }

    public void putSubPackage(List<byte[]> subpackage) {
        for (byte[] bytes : subpackage) {
            packages.offer(bytes);
        }
    }

    private void send() {
        byte[] first = packages.poll();
        if (first != null) {
            characteristic.setValue(first);
            bluetoothGattServer.notifyCharacteristicChanged(connectedDevice, characteristic, true);
        }
    }

    @Override
    public boolean isIdle() {
        return idle.get();
    }
}
