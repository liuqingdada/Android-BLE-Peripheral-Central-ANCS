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
import com.suhen.android.libble.central.callback.BaseCentral;
import com.suhen.android.libble.central.callback.BleBaseCallback;
import com.suhen.android.libble.central.sdk.BleScanRecord;
import com.suhen.android.libble.permission.PermissionWizard;
import com.yanzhenjie.permission.Permission;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */
public abstract class BleCentral extends BaseCentral implements ICentral {
    private static final String TAG = BleCentral.class.getSimpleName();
    protected static final int TRANSPORT_AUTO = 0;
    protected static final int PHY_LE_1M_MASK = 1;

    private BluetoothStatusReceiver mBluetoothStatusReceiver;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private BluetoothLeScanner mLeScanner;

    private HandlerThread mGattCallbackThread;
    private static final int MSG_BLE_SCAN_STOP = 0xFFFF;
    private static final int MSG_BLE_RECONNECT = 0xFFFE;
    private Handler mGattCallbackHandler;

    private Set<BleBaseCallback> mBleBaseCallbacks = new ConcurrentSkipListSet<>();

    private HandlerThread mGattSendDataThread;
    private Handler mGattSendDataHandler;

    protected Context mContext;

    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;

    protected BluetoothGatt mBluetoothGatt;
    //
    protected BluetoothDevice mConnectionDevice;
    private int reconnectCount;
    private boolean autoConnect;
    private int transport = TRANSPORT_AUTO;
    private int phy = PHY_LE_1M_MASK;
    //

    protected BleCentral(Context context) {
        mContext = context;
        mGattCallbackThread = new HandlerThread("gatt_callback_looper_thread");
        mGattCallbackThread.start();

        mGattSendDataThread = new HandlerThread("gatt_send_data_looper_thread");
        mGattSendDataThread.start();

        mGattCallbackHandler = new Handler(mGattCallbackThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleGattCallbackMessage(msg);
            }
        };

        mGattSendDataHandler = new Handler(mGattSendDataThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleGattSendDataMessage(msg);
            }
        };
    }

    private void handleGattCallbackMessage(Message msg) {
        switch (msg.what) {
            case MSG_BLE_SCAN_STOP:
                stopLeScan();
                break;

            case MSG_BLE_RECONNECT:
                tryReconnect();
                break;
        }
    }

    private void handleGattSendDataMessage(Message msg) {
    }

    @SuppressLint("ShowToast")
    @Override
    public void onCreate() {
        mBluetoothStatusReceiver = new BluetoothStatusReceiver();
        IntentFilter bleIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothStatusReceiver, bleIntentFilter);
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
    public void addBleBaseCallback(BleBaseCallback bleBaseCallback) {
        mBleBaseCallbacks.add(bleBaseCallback);
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
        this.autoConnect = autoConnect;
        this.transport = transport;
        this.phy = phy;

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
            mGattCallbackHandler.post(() -> onConnectFailed(mBluetoothGatt, 0, false));

        } else {
            // connect started
            mGattCallbackHandler.post(() -> onConnectStarted(mBluetoothGatt));
        }


        return mBluetoothGatt;
    }

    @Override
    public void preparePair() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mBluetoothStatusReceiver);
        mGattCallbackThread.quitSafely();
        mGattSendDataThread.quitSafely();
    }

    @Override
    public void sendBleBytes(byte[] bytes) {

    }

    private void tryReconnect() {
        if (mConnectionDevice != null) {
            if (reconnectCount > 0) {
                reconnectCount--;
                connect(mConnectionDevice, autoConnect, transport, phy);
            } else {
                if (isFactoryReset()) {
                    factoryReset();
                }
            }
        }
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
        reconnectCount = tryReconnectCount();
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
            mGattCallbackHandler.post(() -> onScannedPeripheral(callbackType, result, null));
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
        mGattCallbackHandler.post(() -> onScannedPeripheral(0, null, bleScanRecord));
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
                        onConnectFailed(gatt, status, true);
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

                    mGattCallbackHandler.sendEmptyMessage(MSG_BLE_RECONNECT);
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: status = " + status);
            mBluetoothGatt = gatt;

            mGattCallbackHandler.post(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    reconnectCount = tryReconnectCount();
                    onConnected(gatt, status);

                } else {
                    stop();
                    onConnectFailed(gatt, status, true);
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead: characteristic = " + characteristic + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                String uuid = characteristic.getUuid().toString();
                for (BleBaseCallback bleBaseCallback : mBleBaseCallbacks) {
                    if (bleBaseCallback.getUUID().equalsIgnoreCase(uuid)) {
                        bleBaseCallback.onCharacteristicRead(characteristic.getValue(), status);
                    }
                }
            });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: characteristic = " + characteristic + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                String uuid = characteristic.getUuid().toString();
                for (BleBaseCallback bleBaseCallback : mBleBaseCallbacks) {
                    if (bleBaseCallback.getUUID().equalsIgnoreCase(uuid)) {
                        bleBaseCallback.onCharacteristicWrite(characteristic.getValue(), status);
                    }
                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged: characteristic = " + characteristic);
            mGattCallbackHandler.post(() -> {
                String uuid = characteristic.getUuid().toString();
                for (BleBaseCallback bleBaseCallback : mBleBaseCallbacks) {
                    if (bleBaseCallback.getUUID().equalsIgnoreCase(uuid)) {
                        bleBaseCallback.onCharacteristicChanged(characteristic.getValue());
                    }
                }
            });
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorRead: descriptor = " + descriptor + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                String uuid = descriptor.getUuid().toString();
                for (BleBaseCallback bleBaseCallback : mBleBaseCallbacks) {
                    if (bleBaseCallback.getUUID().equalsIgnoreCase(uuid)) {
                        bleBaseCallback.onDescriptorRead(descriptor.getValue(), status);
                    }
                }
            });
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite: descriptor = " + descriptor + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                String uuid = descriptor.getUuid().toString();
                for (BleBaseCallback bleBaseCallback : mBleBaseCallbacks) {
                    if (bleBaseCallback.getUUID().equalsIgnoreCase(uuid)) {
                        bleBaseCallback.onDescriptorWrite(descriptor.getValue(), status);
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
                for (BleBaseCallback bleBaseCallback : mBleBaseCallbacks) {
                    bleBaseCallback.onReadRemoteRssi(rssi, status);
                }
            });
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged: mtu = " + mtu + ", status = " + status);
            mGattCallbackHandler.post(() -> {
                for (BleBaseCallback bleBaseCallback : mBleBaseCallbacks) {
                    bleBaseCallback.onMtuChanged(mtu, status);
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

    /**
     *
     */
    public class OperateGattService {
        private BluetoothGattService mGattService;
        private BluetoothGattCharacteristic mCharacteristic;
        private String mDescriptor;

        public OperateGattService(String serviceUUID, String characteristicUUID) {
            if (serviceUUID != null) {
                mGattService = mBluetoothGatt.getService(UUID.fromString(serviceUUID));
            }
            if (characteristicUUID != null) {
                mCharacteristic = mGattService.getCharacteristic(UUID.fromString(characteristicUUID));
            }
        }

        public OperateGattService(String serviceUUID, String characteristicUUID, String descriptorUUID) {
            this(serviceUUID, characteristicUUID);
            mDescriptor = descriptorUUID;
        }

        /**
         * notify
         */
        public void enableCharacteristicNotify(String uuid_notify, boolean userCharacteristicDescriptor) {
            if (mCharacteristic != null && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                setCharacteristicNotification(mBluetoothGatt, mCharacteristic, userCharacteristicDescriptor, true);
            }
        }

        /**
         * stop notify
         */
        public boolean disableCharacteristicNotify(boolean useCharacteristicDescriptor) {
            if (mCharacteristic != null && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                return setCharacteristicNotification(mBluetoothGatt, mCharacteristic, useCharacteristicDescriptor, false);
            } else {
                return false;
            }
        }

        /**
         * notify setting
         */
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
                descriptor = characteristic.getDescriptor(UUID.fromString(mDescriptor));
            }
            if (descriptor == null) {
                return false;
            } else {
                descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                return gatt.writeDescriptor(descriptor);
            }
        }

        /**
         * indicate
         */
        public void enableCharacteristicIndicate(String uuid_indicate, boolean useCharacteristicDescriptor) {
            if (mCharacteristic != null && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                setCharacteristicIndication(mBluetoothGatt, mCharacteristic, useCharacteristicDescriptor, true);
            }
        }


        /**
         * stop indicate
         */
        public boolean disableCharacteristicIndicate(boolean userCharacteristicDescriptor) {
            if (mCharacteristic != null && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                return setCharacteristicIndication(mBluetoothGatt, mCharacteristic, userCharacteristicDescriptor, false);
            } else {
                return false;
            }
        }

        /**
         * indicate setting
         */
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
                descriptor = characteristic.getDescriptor(UUID.fromString(mDescriptor));
            }
            if (descriptor == null) {
                return false;
            } else {
                descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                return gatt.writeDescriptor(descriptor);
            }
        }

        /**
         * write
         */
        public void writeCharacteristic(byte[] data, String uuid_write) {
            if (data == null || data.length <= 0) {
                return;
            }

            if (mCharacteristic == null || (mCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
                return;
            }

            if (mCharacteristic.setValue(data)) {
                if (!mBluetoothGatt.writeCharacteristic(mCharacteristic)) {
                }
            }
        }

        /**
         * read
         */
        public void readCharacteristic(String uuid_read) {
            if (mCharacteristic != null && (mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                if (!mBluetoothGatt.readCharacteristic(mCharacteristic)) {
                }
            } else {
            }
        }

        /**
         * rssi
         */
        public void readRemoteRssi() {
            if (!mBluetoothGatt.readRemoteRssi()) {
            }
        }

        /**
         * set mtu
         */
        public void setMtu(int requiredMtu) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!mBluetoothGatt.requestMtu(requiredMtu)) {
                }
            } else {
            }
        }

        /**
         * requestConnectionPriority
         *
         * @param connectionPriority Request a specific connection priority. Must be one of
         *                           {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED},
         *                           {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}
         *                           or {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
         * @throws IllegalArgumentException If the parameters are outside of their
         *                                  specified range.
         */
        public boolean requestConnectionPriority(int connectionPriority) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return mBluetoothGatt.requestConnectionPriority(connectionPriority);
            }
            return false;
        }
    }
}
