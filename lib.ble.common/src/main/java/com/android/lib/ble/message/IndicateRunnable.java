package com.android.lib.ble.message;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by cooper
 * 20-10-20.
 * Email: 1239604859@qq.com
 */
public class IndicateRunnable implements Runnable {
    private final BluetoothGattServer bluetoothGattServer;
    private final BluetoothDevice connectedDevice;
    private final BluetoothGattCharacteristic characteristic;

    private final ConcurrentLinkedQueue<byte[]> packages = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean quit = new AtomicBoolean(false);
    private volatile Thread t;

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
        if (t == null) {
            t = Thread.currentThread();
            while (!packages.isEmpty()) {
                if (quit.get()) {
                    break;
                }
                send();
                LockSupport.park(this);
            }
        }
    }

    public synchronized void next() {
        LockSupport.unpark(t);
    }

    public void putSubPackage(List<byte[]> subpackage) {
        for (byte[] bytes : subpackage) {
            packages.offer(bytes);
        }
    }

    public void quit() {
        if (quit.compareAndSet(false, true)) {
            if (t != null) {
                t.interrupt();
                t = null;
            }
        }
        packages.clear();
    }

    private void send() {
        byte[] first = packages.poll();
        if (first != null) {
            characteristic.setValue(first);
            bluetoothGattServer.notifyCharacteristicChanged(connectedDevice, characteristic, true);
        }
    }
}
