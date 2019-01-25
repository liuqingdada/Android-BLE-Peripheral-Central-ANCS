package com.suhen.android.appperipheral.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.suhen.android.appperipheral.R;
import com.suhen.android.libble.BLE;
import com.suhen.android.libble.peripheral.IPeripheral;
import com.suhen.android.libble.peripheral.callback.BasePeripheralCallback;
import com.suhen.android.libble.simple.NotificationWizard;

import java.util.Arrays;

public class SimpleBlePeripheralService extends Service {
    private static final String TAG = SimpleBlePeripheralService.class.getSimpleName();
    private static final int FOREGROUND_ID = 0xFFFF;

    private PeripheralBinder mPeripheralBinder;
    private IPeripheral mPeripheral;

    public SimpleBlePeripheralService() {
    }

    public class PeripheralBinder extends Binder {
        public SimpleBlePeripheralService getService() {
            return SimpleBlePeripheralService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mPeripheralBinder == null) {
            mPeripheralBinder = new PeripheralBinder();
        }
        return mPeripheralBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        startForegroound();

        try {
            mPeripheral = BLE.newPeripheral(BlePeripheralImpl.class, this);
            mPeripheral.onCreate();
            mPeripheral.addBasePeripheralCallback(mBasePeripheralCallback);
            mPeripheral.setup();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        return START_STICKY;
    }

    private void startForegroound() {
        Log.d(TAG, "startForeground: ");

        startForeground(FOREGROUND_ID, NotificationWizard.generateNotification(
                this, NotificationManagerCompat.IMPORTANCE_DEFAULT, R.mipmap.ic_launcher,
                getClass(), 1));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        mPeripheral.onDestroy();
    }

    private BasePeripheralCallback mBasePeripheralCallback = new BasePeripheralCallback(BlePeripheralImpl.SERVICE_UUID, BlePeripheralImpl.CHAR_WRITE_UUID) {
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, "onCharacteristicWriteRequest: device = " + device + "\n" +
                    "characteristic = " + characteristic.getUuid().toString() + "\n" +
                    "preparedWrite = " + preparedWrite + "\n" +
                    "responseNeeded = " + responseNeeded + "\n" +
                    "offset = " + offset + "\n" +
                    "value = " + Arrays.toString(value));
            mPeripheral.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }
    };

    private BasePeripheralCallback mBasePeripheralCallback2 = new BasePeripheralCallback(BlePeripheralImpl.SERVICE_UUID, BlePeripheralImpl.CHAR_INDICATE_UUID) {
        @Override
        public void onNotificationSent(BluetoothDevice device, BluetoothGattCharacteristic characteristic, int status) {

        }
    };

    public IPeripheral getPeripheral() {
        return mPeripheral;
    }
}
