package com.suhen.android.libble.central;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.R;
import com.suhen.android.libble.central.base.BleBaseCentral;
import com.suhen.android.libble.central.callback.BaseCentralCallback;
import com.suhen.android.libble.central.callback.CentralStatusCallback;
import com.suhen.android.libble.central.sdk.BleScanRecord;
import com.suhen.android.libble.permission.PermissionWizard;
import com.yanzhenjie.permission.Permission;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */
public abstract class BleCentral extends BleBaseCentral implements ICentral {
    private static final String TAG = BleCentral.class.getSimpleName();

    private BluetoothStatusReceiver mBluetoothStatusReceiver;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private BluetoothLeScanner mLeScanner;

    private HandlerThread mGattCallbackThread;
    private static final int MSG_BLE_SCAN_STOP = 0xFFFF;
    private Handler mGattCallbackHandler;

    protected List<BaseCentralCallback> mBaseCentralCallbacks = new CopyOnWriteArrayList<>();

    private HandlerThread mGattReadWriteThread;
    private Handler mGattReadWriteHandler;

    protected Context mContext;
    protected CentralStatusCallback mCentralStatusCallback;

    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;

    protected BluetoothGatt mBluetoothGatt;
    protected BluetoothDevice mConnectionDevice;

    protected BleCentral(Context context) {
        mContext = context;
        mGattCallbackThread = new HandlerThread("gatt_callback_looper_thread");
        mGattCallbackThread.start();

        mGattReadWriteThread = new HandlerThread("gatt_read_write_looper_thread");
        mGattReadWriteThread.start();

        mGattCallbackHandler = new Handler(mGattCallbackThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleGattCallbackMessage(msg);
            }
        };

        mGattReadWriteHandler = new Handler(mGattReadWriteThread.getLooper());
    }

    private void handleGattCallbackMessage(Message msg) {
        switch (msg.what) {
            case MSG_BLE_SCAN_STOP:
                stopLeScan();
                break;
        }
    }

    @SuppressLint("ShowToast")
    @Override
    public void onCreate() {
        mBluetoothStatusReceiver = new BluetoothStatusReceiver();
        IntentFilter bleIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothStatusReceiver, bleIntentFilter);
    }

    @Override
    public void setCentralStatusCallback(CentralStatusCallback centralStatusCallback) {

    }

    @Override
    public void setup() {
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.d(TAG, "setup: " + BLE.NOT_SUPPORT_PERIPHERAL);
            return;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.d(TAG, "setup: BluetoothAdapter is null, " + BLE.NOT_SUPPORT_PERIPHERAL);
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) { // if bt not enable

            if (!mBluetoothAdapter.enable()) { // if bt open fail
                openBTByUser();
            }

        } else {
            start();
        }
    }

    @Override
    public void scan() {
        start();
    }

    @Override
    public void stopScan() {
        mGattCallbackHandler.removeMessages(MSG_BLE_SCAN_STOP);
        stopLeScan();
    }

    @Override
    public void addBleBaseCallback(BaseCentralCallback baseCentralCallback) {
        mBaseCentralCallbacks.add(baseCentralCallback);
    }

    /**
     * @param transport Default is {@link BluetoothDevice#TRANSPORT_AUTO},
     *                  According to the practical peripherals to adjust
     *                  <p>
     *                  <p/>
     * @param phy       Default is {@link BluetoothDevice#PHY_LE_1M_MASK},
     *                  According to the practical peripherals to adjust
     */
    @Override
    public synchronized BluetoothGatt connect(BluetoothDevice bluetoothDevice, boolean autoConnect, int transport, int phy) {
        mConnectionDevice = bluetoothDevice;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBluetoothGatt = bluetoothDevice.connectGatt(mContext, autoConnect, mBluetoothGattCallback, transport);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = bluetoothDevice.connectGatt(mContext, autoConnect, mBluetoothGattCallback, transport);

        } else {
            mBluetoothGatt = bluetoothDevice.connectGatt(mContext, autoConnect, mBluetoothGattCallback);
        }

        if (mBluetoothGatt == null) {
            stop();

            // connect failed
            mGattCallbackHandler.post(() -> onConnectFailed(mBluetoothGatt, CONNECT_ERROR_GATT_UNKNOWN));

        } else {
            // connect started
            mGattCallbackHandler.post(() -> onConnectStarted(mBluetoothGatt));
        }


        return mBluetoothGatt;
    }

    @Override
    public boolean isConnected() {
        if (mBluetoothManager != null && mConnectionDevice != null) {
            return mBluetoothManager.getConnectionState(mConnectionDevice, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED;
        }
        return false;
    }

    @Override
    public IOperator newOperator(String serviceUUID, String characteristicUUID) {
        return new GattServiceOperator(serviceUUID, characteristicUUID);
    }

    @Override
    public IOperator newOperator(String serviceUUID, String characteristicUUID, String descriptorUUID) {
        return new GattServiceOperator(serviceUUID, characteristicUUID, descriptorUUID);
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mBluetoothStatusReceiver);
        mGattCallbackThread.quitSafely();
        mGattReadWriteThread.quitSafely();
        mBaseCentralCallbacks.clear();
    }

    private class BluetoothStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(TAG, "bluetooth OFF");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.e(TAG, "bluetooth TURNING_OFF");
                        stop();
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.e(TAG, "bluetooth ON");
                        start();

                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.e(TAG, "bluetooth TURNING_ON");
                        break;
                }
            }
        }
    }

    private synchronized void start() {
        PermissionWizard.requestPermission(mContext, new PermissionWizard.PermissionCallback() {
            @Override
            public void onGranted() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    scanLollipop();
                } else {
                    scanJellyBeanMr2();
                }
            }
        }, Permission.Group.LOCATION);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void scanLollipop() {
        List<ScanFilter> filters = null;
        List<ScanFilter> scanFilters = scanLollipopFilters();
        if (scanFilters != null) {
            filters = new ArrayList<>(scanFilters);
        }

        if (mBluetoothAdapter.isEnabled()) {
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mLeScanner.startScan(filters, new ScanSettings.Builder().build(), mLeLollipopCallback);
            onScanStarted();

            int scanTime = scanTimeout() <= 0 ? DEFAULT_SCAN_TIMEOUT : scanTimeout();
            mGattCallbackHandler.removeMessages(MSG_BLE_SCAN_STOP);
            mGattCallbackHandler.sendEmptyMessageDelayed(MSG_BLE_SCAN_STOP, scanTime * 1000);
        }
    }

    private void scanJellyBeanMr2() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.startLeScan(scanFilters(), mLeCallback);
            onScanStarted();

            int scanTime = scanTimeout() <= 0 ? DEFAULT_SCAN_TIMEOUT : scanTimeout();
            mGattCallbackHandler.removeMessages(MSG_BLE_SCAN_STOP);
            mGattCallbackHandler.sendEmptyMessageDelayed(MSG_BLE_SCAN_STOP, scanTime * 1000);
        }
    }

    private void stopLeScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mLeScanner.flushPendingScanResults(mLeLollipopCallback);
            mLeScanner.stopScan(mLeLollipopCallback);
            onScanFinished();
        } else {
            mBluetoothAdapter.stopLeScan(mLeCallback);
            onScanFinished();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback mLeLollipopCallback = new ScanCallback() {
        /**
         * Callback when a BLE advertisement has been found.
         *
         * @param callbackType Determines how this callback was triggered.
         *                     Could be one of {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
         *                     {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
         *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
         * @param result A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mGattCallbackHandler.post(() -> onScannedPeripheral(result, null, result.getDevice(), result.getRssi()));
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeCallback = (device, rssi, scanRecord) -> {
        BleScanRecord bleScanRecord = BleScanRecord.parseFromBytes(scanRecord);
        mGattCallbackHandler.post(() -> onScannedPeripheral(null, bleScanRecord, device, rssi));
    };

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.d(TAG, "onPhyUpdate: txPhy = " + txPhy + ", rxPhy = " + rxPhy + "status = " + status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.d(TAG, "onPhyRead: txPhy = " + txPhy + ", rxPhy = " + rxPhy + "status = " + status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange: status = " + status + ", newState = " + newState);
            mBluetoothGatt = gatt;
            mGattCallbackHandler.post(() -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "onConnectionStateChange: STATE_CONNECTED");

                    SystemClock.sleep(500);
                    boolean discoverServices = mBluetoothGatt.discoverServices();
                    if (!discoverServices) {
                        stop();
                        onConnectFailed(gatt, status);
                    }

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "onConnectionStateChange: STATE_DISCONNECTED");
                    stop();
                    onDisconnected(gatt);

                } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                    Log.i(TAG, "onConnectionStateChange: STATE_CONNECTING");

                } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                    Log.i(TAG, "onConnectionStateChange: STATE_DISCONNECTING");

                } else {
                    Log.i(TAG, "onConnectionStateChange: ERROR");
                    if (isFactoryReset()) {
                        factoryReset();
                    }
                    onConnectFailed(gatt, newState);
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: status = " + status);
            mBluetoothGatt = gatt;

            mGattCallbackHandler.post(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onConnected(gatt, status);

                } else {
                    stop();
                    onConnectFailed(gatt, status);
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead: characteristic = " + characteristic + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                String uuid = characteristic.getUuid().toString();
                for (BaseCentralCallback baseCentralCallback : mBaseCentralCallbacks) {
                    if (baseCentralCallback.getParentUuid().equalsIgnoreCase(characteristic.getService().getUuid().toString()) &&
                            baseCentralCallback.getChildUuid().equalsIgnoreCase(uuid)) {
                        baseCentralCallback.onCharacteristicRead(characteristic.getValue(), status);
                    }
                }
            });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: characteristic = " + characteristic + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                String uuid = characteristic.getUuid().toString();
                for (BaseCentralCallback baseCentralCallback : mBaseCentralCallbacks) {
                    if (baseCentralCallback.getParentUuid().equalsIgnoreCase(characteristic.getService().getUuid().toString()) &&
                            baseCentralCallback.getChildUuid().equalsIgnoreCase(uuid)) {
                        baseCentralCallback.onCharacteristicWrite(characteristic.getValue(), status);
                    }
                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged: characteristic = " + characteristic);
            mGattCallbackHandler.post(() -> {
                String uuid = characteristic.getUuid().toString();
                for (BaseCentralCallback baseCentralCallback : mBaseCentralCallbacks) {
                    if (baseCentralCallback.getParentUuid().equalsIgnoreCase(characteristic.getService().getUuid().toString()) &&
                            baseCentralCallback.getChildUuid().equalsIgnoreCase(uuid)) {
                        baseCentralCallback.onCharacteristicChanged(characteristic.getValue());
                    }
                }
            });
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorRead: mBluetoothGattDescriptor = " + descriptor + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                String uuid = descriptor.getUuid().toString();
                for (BaseCentralCallback baseCentralCallback : mBaseCentralCallbacks) {
                    if (baseCentralCallback.getParentUuid().equalsIgnoreCase(descriptor.getCharacteristic().getUuid().toString()) &&
                            baseCentralCallback.getChildUuid().equalsIgnoreCase(uuid)) {
                        baseCentralCallback.onDescriptorRead(descriptor.getValue(), status);
                    }
                }
            });
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite: mBluetoothGattDescriptor = " + descriptor + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                String uuid = descriptor.getUuid().toString();
                for (BaseCentralCallback baseCentralCallback : mBaseCentralCallbacks) {
                    if (baseCentralCallback.getParentUuid().equalsIgnoreCase(descriptor.getCharacteristic().getUuid().toString()) &&
                            baseCentralCallback.getChildUuid().equalsIgnoreCase(uuid)) {
                        baseCentralCallback.onDescriptorWrite(descriptor.getValue(), status);
                    }
                }
            });
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onReliableWriteCompleted: status = " + status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "onReadRemoteRssi: rssi = " + rssi + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                for (BaseCentralCallback baseCentralCallback : mBaseCentralCallbacks) {
                    baseCentralCallback.onReadRemoteRssi(rssi, status);
                }
            });
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged: mtu = " + mtu + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                for (BaseCentralCallback baseCentralCallback : mBaseCentralCallbacks) {
                    baseCentralCallback.onMtuChanged(mtu, status);
                }
            });
        }
    };

    private synchronized void stop() {
        disconnectGatt();
        refreshDeviceCache();
        closeBluetoothGatt();
    }

    private void openBTByUser() {
        if (!mBluetoothAdapter.isEnabled()) {
            permissionDenied();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(enableBtIntent);
        }
    }

    private void permissionDenied() {
        Toast.makeText(mContext, BLE.BT_NO_PERMISSION, Toast.LENGTH_LONG).show();
    }

    private synchronized void disconnectGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    private synchronized void refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (mBluetoothGatt != null) {
                boolean success = (Boolean) refresh.invoke(mBluetoothGatt);
                Log.i(TAG, "refreshDeviceCache, is success:  " + success);
            }
        } catch (Exception e) {
            Log.w(TAG, "refreshDeviceCache: ", e);
            e.printStackTrace();
        }
    }

    private synchronized void factoryReset() {
        try {
            final Method factoryReset = BluetoothAdapter.class.getMethod("factoryReset");
            if (mBluetoothAdapter != null) {
                boolean success = (boolean) factoryReset.invoke(mBluetoothAdapter);
                Log.i(TAG, "factoryReset: is success: " + success);
            }

        } catch (Exception e) {
            Log.w(TAG, "factoryReset: ", e);
            Toast.makeText(mContext, R.string.message_gatt_error, Toast.LENGTH_LONG).show();
        }
    }

    private synchronized void closeBluetoothGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
    }

    protected class GattServiceOperator extends AbstractOperator {
        private BluetoothGattService mBluetoothGattService;
        private BluetoothGattCharacteristic mBluetoothGattCharacteristic;
        private BluetoothGattDescriptor mBluetoothGattDescriptor;

        private GattServiceOperator(String serviceUUID, String characteristicUUID) {
            if (serviceUUID != null) {
                mBluetoothGattService = mBluetoothGatt.getService(UUID.fromString(serviceUUID));
            }
            if (characteristicUUID != null) {
                mBluetoothGattCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(characteristicUUID));
            }
        }

        private GattServiceOperator(String serviceUUID, String characteristicUUID, String descriptorUUID) {
            this(serviceUUID, characteristicUUID);
            mBluetoothGattDescriptor = mBluetoothGattCharacteristic.getDescriptor(UUID.fromString(descriptorUUID));
        }

        @Override
        public void enableCharacteristicNotify(boolean userCharacteristicDescriptor) {
            mGattReadWriteHandler.post(() -> {
                if (mBluetoothGattCharacteristic != null && (mBluetoothGattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    boolean set = setCharacteristicNotification(mBluetoothGatt, mBluetoothGattCharacteristic, userCharacteristicDescriptor, true);
                    Log.v(TAG, "enableCharacteristicNotify: " + set);
                } else {
                    Log.v(TAG, "enableCharacteristicNotify: error false");
                }
            });
        }

        @Override
        public void disableCharacteristicNotify(boolean useCharacteristicDescriptor) {
            mGattReadWriteHandler.post(() -> {
                if (mBluetoothGattCharacteristic != null && (mBluetoothGattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    boolean set = setCharacteristicNotification(mBluetoothGatt, mBluetoothGattCharacteristic, useCharacteristicDescriptor, false);
                    Log.v(TAG, "disableCharacteristicNotify: " + set);
                } else {
                    Log.v(TAG, "disableCharacteristicNotify: error false");
                }
            });
        }

        private boolean setCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                boolean useCharacteristicDescriptor, boolean enable) {
            if (gatt == null || characteristic == null) {
                return false;
            }

            boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
            if (!success1) {
                return false;
            }

            BluetoothGattDescriptor descriptor;
            if (useCharacteristicDescriptor) {
                descriptor = characteristic.getDescriptor(characteristic.getUuid());
            } else {
                descriptor = characteristic.getDescriptor(UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
            }
            if (descriptor == null) {
                return false;
            } else {
                descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                return gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void enableCharacteristicIndicate(boolean useCharacteristicDescriptor) {
            mGattReadWriteHandler.post(() -> {
                if (mBluetoothGattCharacteristic != null && (mBluetoothGattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    boolean set = setCharacteristicIndication(mBluetoothGatt, mBluetoothGattCharacteristic, useCharacteristicDescriptor, true);
                    Log.v(TAG, "enableCharacteristicIndicate: " + set);
                } else {
                    Log.v(TAG, "enableCharacteristicIndicate: error false");
                }
            });
        }

        @Override
        public void disableCharacteristicIndicate(boolean userCharacteristicDescriptor) {
            mGattReadWriteHandler.post(() -> {
                if (mBluetoothGattCharacteristic != null && (mBluetoothGattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    boolean set = setCharacteristicIndication(mBluetoothGatt, mBluetoothGattCharacteristic, userCharacteristicDescriptor, false);
                    Log.v(TAG, "disableCharacteristicIndicate: " + set);
                } else {
                    Log.v(TAG, "disableCharacteristicIndicate: error false");
                }
            });
        }

        private boolean setCharacteristicIndication(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                boolean useCharacteristicDescriptor, boolean enable) {
            if (gatt == null || characteristic == null) {
                return false;
            }

            boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
            if (!success1) {
                return false;
            }

            BluetoothGattDescriptor descriptor;
            if (useCharacteristicDescriptor) {
                descriptor = characteristic.getDescriptor(characteristic.getUuid());
            } else {
                descriptor = characteristic.getDescriptor(UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
            }
            if (descriptor == null) {
                return false;
            } else {
                descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                return gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void writeCharacteristic(byte[] data) {
            mGattReadWriteHandler.post(() -> {
                if (data == null || data.length <= 0) {
                    Log.v(TAG, "writeCharacteristic: data null");
                    return;
                }

                if (mBluetoothGattCharacteristic == null || (mBluetoothGattCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
                    Log.v(TAG, "writeCharacteristic: mBluetoothGattCharacteristic null");
                    return;
                }

                mBluetoothGattCharacteristic.setValue(data);
                boolean wtite = mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
                Log.v(TAG, "writeCharacteristic: " + wtite);
            });
        }

        @Override
        public void readCharacteristic() {
            mGattReadWriteHandler.post(() -> {
                if (mBluetoothGattCharacteristic != null && (mBluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    boolean read = mBluetoothGatt.readCharacteristic(mBluetoothGattCharacteristic);
                    Log.v(TAG, "readCharacteristic: " + read);
                } else {
                    Log.v(TAG, "readCharacteristic: error false");
                }
            });
        }

        @Override
        public void writeDescriptor(byte[] data) {
            mGattReadWriteHandler.post(() -> {
                if (data == null || data.length <= 0) {
                    Log.v(TAG, "writeDescriptor: data null");
                    return;
                }

                if (mBluetoothGattDescriptor == null) {
                    Log.v(TAG, "writeDescriptor: mBluetoothGattDescriptor null");
                    return;
                }

                if (mBluetoothGattDescriptor.setValue(data)) {
                    boolean write = mBluetoothGatt.writeDescriptor(mBluetoothGattDescriptor);
                    Log.v(TAG, "writeDescriptor: " + write);
                } else {
                    Log.v(TAG, "writeDescriptor: error false");
                }
            });
        }

        @Override
        public void readDescriptor() {
            mGattReadWriteHandler.post(() -> {
                if (mBluetoothGattDescriptor == null) {
                    Log.v(TAG, "readDescriptor: mBluetoothGattDescriptor null");
                    return;
                }

                boolean read = mBluetoothGatt.readDescriptor(mBluetoothGattDescriptor);
                Log.v(TAG, "readDescriptor: " + read);
            });
        }

        @Override
        public void readRemoteRssi() {
            mGattReadWriteHandler.post(() -> {
                boolean read = mBluetoothGatt.readRemoteRssi();
                Log.v(TAG, "readRemoteRssi: " + read);
            });
        }

        @Override
        public void setMtu(int requiredMtu) {
            mGattReadWriteHandler.post(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    boolean req = mBluetoothGatt.requestMtu(requiredMtu);
                    Log.v(TAG, "setMtu: " + req);
                } else {
                    Log.v(TAG, "setMtu: require LOLLIPOP");
                }
            });
        }

        /**
         * @param connectionPriority Request a specific connection priority. Must be one of
         *                           {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED},
         *                           {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}
         *                           or {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
         * @throws IllegalArgumentException If the parameters are outside of their
         *                                  specified range.
         */
        @Override
        public void requestConnectionPriority(int connectionPriority) {
            mGattReadWriteHandler.post(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    boolean req = mBluetoothGatt.requestConnectionPriority(connectionPriority);
                    Log.v(TAG, "requestConnectionPriority: " + req);
                } else {
                    Log.v(TAG, "requestConnectionPriority: require LOLLIPOP");
                }
            });
        }
    }

    @Override
    public String getCharacteristicProperty(BluetoothGattCharacteristic characteristic) {
        int charaProp = characteristic.getProperties();
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0) {
            return "Broadcast";
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            return "Read";
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
            return "Write No Response";
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            return "Write";
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return "Notify";
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            return "Indicate";
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) > 0) {
            return "Signed Write";
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) > 0) {
            return "Extended";
        }
        return "Unknown";
    }

    @Override
    public int getIntCharacteristicProperty(BluetoothGattCharacteristic characteristic) {
        int charaProp = characteristic.getProperties();
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0) {
            return BluetoothGattCharacteristic.PROPERTY_BROADCAST;
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            return BluetoothGattCharacteristic.PROPERTY_READ;
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
            return BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            return BluetoothGattCharacteristic.PROPERTY_WRITE;
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return BluetoothGattCharacteristic.PROPERTY_NOTIFY;
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            return BluetoothGattCharacteristic.PROPERTY_INDICATE;
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) > 0) {
            return BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) > 0) {
            return BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS;
        }
        return CHARACTERISTIC_PROPERTY_UNKNOWN;
    }
}
