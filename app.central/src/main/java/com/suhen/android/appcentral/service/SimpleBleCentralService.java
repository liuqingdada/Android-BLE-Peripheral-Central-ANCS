package com.suhen.android.appcentral.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.suhen.android.appcentral.R;
import com.suhen.android.libble.BLE;
import com.suhen.android.libble.central.ICentral;
import com.suhen.android.libble.central.callback.BaseCentralCallback;
import com.suhen.android.libble.central.callback.CentralStatusCallback;
import com.suhen.android.libble.central.sdk.BleScanRecord;
import com.suhen.android.libble.message.BleMessage;
import com.suhen.android.libble.permission.PermissionWizard;
import com.suhen.android.libble.simple.NotificationWizard;

import java.util.Arrays;

public class SimpleBleCentralService extends Service {
    private static final String TAG = SimpleBleCentralService.class.getSimpleName();
    private static final int FOREGROUND_ID = 0xFFFE;
    private BleCentralBinder mBinder;
    private ICentral mCentral;

    public SimpleBleCentralService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new BleCentralBinder();
        }
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        startForegroound();

        try {
            mCentral = BLE.newCentral(BleCentralImpl.class, this);
            mCentral.onCreate();
            mCentral.setCentralStatusCallback(mCentralStatusCallback);
            mCentral.setup();

        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean locationEnable = PermissionWizard.isLocationEnable(this);
        Log.d(TAG, "onCreate: locationEnable = " + locationEnable);
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
        mCentral.onDestroy();
    }

    public class BleCentralBinder extends Binder {
        public SimpleBleCentralService getBleService() {
            return SimpleBleCentralService.this;
        }
    }

    public ICentral getCentral() {
        return mCentral;
    }

    //***********************************************/
    //****************业务逻辑层***********************/
    //***********************************************/
    // UUID
    private static final String SERVICE_UUID = "4622c045-1cd2-4211-adc5-89df72c789ec";
    private static final String CHAR_INDICATE_UUID = "4622c046-1cd2-4211-adc5-89df72c789ec";
    private static final String CHAR_WRITE_UUID = "4622c047-1cd2-4211-adc5-89df72c789ec";
    private static final String CHAR_READ_UUID = "4622c048-1cd2-4211-adc5-89df72c789ec";

    private ICentral.IOperator writeOperator;
    private BaseCentralCallback writeCallback = new BaseCentralCallback(SERVICE_UUID, CHAR_WRITE_UUID) {
        @Override
        public void onCharacteristicWrite(byte[] value, int status) {
            super.onCharacteristicWrite(value, status);
            Log.d(TAG, "onCharacteristicWrite: " + Arrays.toString(value));
        }
    };

    private ICentral.IOperator readOperator;
    private BaseCentralCallback readCallback = new BaseCentralCallback(SERVICE_UUID, CHAR_READ_UUID) {
        @Override
        public void onCharacteristicRead(byte[] value, int status) {
            super.onCharacteristicRead(value, status);
            Log.d(TAG, "onCharacteristicRead: " + Arrays.toString(value));
        }
    };

    private CentralStatusCallback mCentralStatusCallback = new CentralStatusCallback() {
        @Override
        public void onScannedPeripheral(ScanResult result, BleScanRecord bleScanRecord, BluetoothDevice remoteDevice, int rssi) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && result != null) {
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord != null) {
                    String deviceName = scanRecord.getDeviceName();
                    if (deviceName != null) {
                        if (deviceName.contains("Suhen") || remoteDevice.getName().contains("Suhen")) {
                            mCentral.stopScan();
                            mCentral.connect(remoteDevice, false, TRANSPORT_AUTO, PHY_LE_1M_MASK);
                        }
                    }
                }
//                    if ("OTA_FFFFFFFFFFFF".equals(deviceName) || "OTA_FFFFFFFFFFFF".equals(remoteDevice.getName())) {
//                        Log.d(TAG, "onScannedPeripheral: " + result);
//
//                        stopScan();
//
//                        connect(remoteDevice, false, TRANSPORT_AUTO, PHY_LE_1M_MASK);
//                    }

            } else if (bleScanRecord != null) {

            }
        }

        @Override
        public void onConnectStarted(BluetoothGatt bluetoothGatt) {
        }

        @Override
        public void onConnected(BluetoothGatt bluetoothGatt, int status) {
            if (writeOperator == null) {
                writeOperator = mCentral.newOperator(writeCallback.getParentUuid(), writeCallback.getChildUuid());
            }
            if (readOperator == null) {
                readOperator = mCentral.newOperator(readCallback.getParentUuid(), readCallback.getChildUuid());
            }
        }

        @Override
        public void onConnectFailed(BluetoothGatt bluetoothGatt, int status) {
        }

        @Override
        public void onDisconnected(BluetoothGatt bluetoothGatt) {
        }
    };

    public void read() {
        if (mCentral.isConnected() && readOperator != null) {
            readOperator.readCharacteristic();
        }
    }

    /**
     * 这里比较自由了，大一点的数据要分包，而且要发一个包等待响应回调，然后发送下一个包
     * 具体根据业务逻辑定吧
     * 这里有一种简单的方式{@link BleMessage}
     */
    public void write(BleMessage bleMessage) {

    }
}
