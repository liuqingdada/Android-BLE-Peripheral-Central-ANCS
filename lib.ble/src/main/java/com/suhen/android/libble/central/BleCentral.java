package com.suhen.android.libble.central;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.central.callback.BleBaseCallback;
import com.suhen.android.libble.central.callback.BleMtuChangedCallback;
import com.suhen.android.libble.central.callback.BleRssiCallback;
import com.suhen.android.libble.central.sdk.BleScanRecord;
import com.suhen.android.libble.permission.PermissionWizard;
import com.yanzhenjie.permission.Permission;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */
public abstract class BleCentral implements ICentral {
    protected static final String TAG = BleCentral.class.getSimpleName();
    private static final int DEFAULT_SCAN_TIMEOUT = 60;

    protected Context mContext;

    private BluetoothStatusReceiver mBluetoothStatusReceiver;
    private Toast mToast;

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private BluetoothLeScanner mLeScanner;

    protected BluetoothGatt mBluetoothGatt;
    protected BluetoothDevice mConnectionDevice;
    protected Set<BleBaseCallback> mBleBaseCallbacks = new ConcurrentSkipListSet<>();
    protected BleRssiCallback mBleRssiCallback;
    protected BleMtuChangedCallback mBleMtuChangedCallback;

    private HandlerThread mCentralThread;
    private static final int MSG_BLE_LOLLIPOP_SCAN_STOP = 0xFFFF;
    private static final int MSG_BLE_SCAN_STOP = 0xFFFE;
    private Handler mHandler;

    protected BleCentral(Context context) {
        mContext = context;
        mCentralThread = new HandlerThread("central_thread");
        mCentralThread.start();
        mHandler = new Handler(mCentralThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                BleCentral.this.handleMessage(msg);
            }
        };
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_BLE_LOLLIPOP_SCAN_STOP:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mLeScanner.stopScan(mLeLollipopCallback);
                    onScanFinished();
                }
                break;

            case MSG_BLE_SCAN_STOP:
                mBluetoothAdapter.stopLeScan(mLeCallback);
                onScanFinished();
                break;
        }
    }

    @SuppressLint("ShowToast")
    @Override
    public void onCreate() {
        mBluetoothStatusReceiver = new BluetoothStatusReceiver();
        IntentFilter bleIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothStatusReceiver, bleIntentFilter);

        mToast = Toast.makeText(mContext, BLE.NOT_SUPPORT_CENTRAL, Toast.LENGTH_LONG);
    }

    @Override
    public boolean isSupportCentral() {
        if (mContext.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }

            mBluetoothAdapter = mBluetoothManager.getAdapter();

            return mBluetoothAdapter != null;
        } else {
            return false;
        }
    }

    @Override
    public void setup() {
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.d(TAG, "setup: " + BLE.NOT_SUPPORT_PERIPHERAL);
            mToast.show();
            return;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.d(TAG, "setup: BluetoothAdapter is null, " + BLE.NOT_SUPPORT_PERIPHERAL);
            mToast.show();
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
    public void addBleBaseCallback(BleBaseCallback bleBaseCallback) {
        mBleBaseCallbacks.add(bleBaseCallback);
    }

    @Override
    public void setBleRssiCallback(BleRssiCallback bleRssiCallback) {
        mBleRssiCallback = bleRssiCallback;
    }

    @Override
    public void setBleMtuChangedCallback(BleMtuChangedCallback bleMtuChangedCallback) {
        mBleMtuChangedCallback = bleMtuChangedCallback;
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
            mHandler.post(() -> onConnectFailed(mBluetoothGatt, 0, false));

        } else {
            // connect started
            mHandler.post(() -> onConnectStarted(mBluetoothGatt));
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
        mCentralThread.quitSafely();
    }

    @Override
    public void sendBleBytes(byte[] bytes) {

    }

    /**
     * default timeout is 60s
     */
    protected abstract int scanTimeout();

    protected abstract String scanDeviceName();

    protected abstract int manufacturerId();

    protected abstract byte[] manufacturerData();

    protected abstract void onScanStarted();

    protected abstract void onScanFinished();

    protected void onScannedPeripheral(BluetoothDevice bluetoothDevice, int rssi, String deviceName, byte[] manufacturerData) {
    }

    protected abstract void onConnectStarted(BluetoothGatt bluetoothGatt);

    protected abstract void onConnected(BluetoothGatt bluetoothGatt, int status);

    protected abstract void onConnectFailed(BluetoothGatt bluetoothGatt, int status, boolean isGattCallback);

    protected abstract void onDisconnected(BluetoothGatt bluetoothGatt);

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
                        if (isSupportCentral()) {
                            stop();
                        }
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.e(TAG, "bluetooth ON");
                        if (isSupportCentral()) {
                            start();
                        }

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

        String deviceName = scanDeviceName();
        byte[] manufacturerData = manufacturerData();

        if (deviceName != null && manufacturerData != null && manufacturerData.length > 0) {
            filters = new ArrayList<>();

            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setDeviceName(deviceName)
                    .setManufacturerData(manufacturerId(), manufacturerData)
                    .build();

            filters.add(scanFilter);
        }

        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mLeScanner.startScan(filters, new ScanSettings.Builder().build(), mLeLollipopCallback);
        onScanStarted();

        mHandler.removeMessages(MSG_BLE_LOLLIPOP_SCAN_STOP);
        int scanTime = scanTimeout() <= 0 ? DEFAULT_SCAN_TIMEOUT : scanTimeout();
        mHandler.sendEmptyMessageDelayed(MSG_BLE_LOLLIPOP_SCAN_STOP, scanTime);
    }

    private void scanJellyBeanMr2() {
        mBluetoothAdapter.startLeScan(mLeCallback);
        onScanStarted();

        mHandler.removeMessages(MSG_BLE_SCAN_STOP);
        int scanTime = scanTimeout() <= 0 ? DEFAULT_SCAN_TIMEOUT : scanTimeout();
        mHandler.sendEmptyMessageDelayed(MSG_BLE_SCAN_STOP, scanTime);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback mLeLollipopCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord != null) {

                BluetoothDevice device = result.getDevice();

                int rssi = result.getRssi();

                String deviceName = scanRecord.getDeviceName();

                byte[] manufacturerData = scanRecord.getManufacturerSpecificData(manufacturerId());

                mHandler.post(() -> onScannedPeripheral(device, rssi, deviceName, manufacturerData));

            }
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
        BleScanRecord record = BleScanRecord.parseFromBytes(scanRecord);

        if (record != null) {
            String deviceName = record.getDeviceName();

            byte[] manufacturerData = record.getManufacturerSpecificData(manufacturerId());

            mHandler.post(() -> onScannedPeripheral(device, rssi, deviceName, manufacturerData));
        }
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
            mHandler.post(() -> {
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
                    Log.i(TAG, "onConnectionStateChange: UNKNOWN");
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: status = " + status);
            mBluetoothGatt = gatt;

            mHandler.post(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
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
            mHandler.post(() -> {
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
            mHandler.post(() -> {
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
            mHandler.post(() -> {
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
            mHandler.post(() -> {
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
            mHandler.post(() -> {
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
            mHandler.post(() -> {
                if (mBleRssiCallback != null) {
                    mBleRssiCallback.onReadRemoteRssi(rssi, status);
                }
            });
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged: mtu = " + mtu + ", status = " + status);
            mHandler.post(() -> {
                if (mBleMtuChangedCallback != null) {
                    mBleMtuChangedCallback.onMtuChanged(mtu, status);
                }
            });
        }
    };

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

    private synchronized void closeBluetoothGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
    }

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
}
