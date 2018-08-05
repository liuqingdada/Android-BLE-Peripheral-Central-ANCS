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
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.central.sdk.BleScanRecord;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */
public abstract class BleCentral implements ICentral {
    protected static final String TAG = BleCentral.class.getSimpleName();

    /**/

    private int RECONNECT_COUNT = 1;

    /**/

    protected Context mContext;

    private BluetoothStatusReceiver mBluetoothStatusReceiver;
    private Toast mToast;

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private BluetoothLeScanner mLeScanner;

    protected BluetoothGatt bluetoothGatt;

    private static final int MSG_BLE_LOLLIPOP_SCAN_STOP = 0xFFFF;
    private static final int MSG_BLE_SCAN_STOP = 0xFFFE;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
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
    };

    protected BleCentral(Context context) {
        mContext = context;
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

            mBluetoothManager = (BluetoothManager) mContext.getSystemService(
                    Context.BLUETOOTH_SERVICE);
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
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(
                Context.BLUETOOTH_SERVICE);
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

    /**
     * @param transport Default is {@link BluetoothDevice#TRANSPORT_AUTO},
     *                  According to the practical peripherals to adjust
     *                  <p>
     *                  <p/>
     * @param phy       Default is {@link BluetoothDevice#PHY_LE_1M_MASK},
     *                  According to the practical peripherals to adjust
     */
    @Override
    public synchronized BluetoothGatt connect(BluetoothDevice bluetoothDevice,
                                              boolean autoConnect,
                                              int transport,
                                              int phy) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bluetoothGatt = bluetoothDevice.connectGatt(mContext, autoConnect,
                                                        mBluetoothGattCallback,
                                                        transport);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = bluetoothDevice.connectGatt(mContext, autoConnect,
                                                        mBluetoothGattCallback, transport);

        } else {
            bluetoothGatt = bluetoothDevice.connectGatt(mContext, autoConnect,
                                                        mBluetoothGattCallback);
        }

        if (bluetoothGatt == null) {
            disconnectGatt();
            refreshDeviceCache();
            closeBluetoothGatt();

            // connect failed
            onConnectFailed(bluetoothGatt, bluetoothDevice);

        } else {
            // connect started
            onConnectStarted(bluetoothGatt, bluetoothDevice);
        }


        return bluetoothGatt;
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
    }

    @Override
    public void sendBleBytes(byte[] bytes) {

    }

    /**
     * default timeout is 60s
     */
    protected abstract int scanTimeout();

    /**
     * default repeat time is 1
     */
    protected abstract int reconnectCount();

    protected abstract String scanDeviceName();

    protected abstract int manufacturerId();

    protected abstract byte[] manufacturerData();

    protected abstract void onScanStarted();

    protected abstract void onScanFinished();

    protected void onScannedPeripheral(BluetoothDevice bluetoothDevice,
                                       int rssi,
                                       String deviceName,
                                       byte[] manufacturerData) {
    }

    protected abstract void onConnectStarted(BluetoothGatt bluetoothGatt,
                                             BluetoothDevice bluetoothDevice);

    protected abstract void onConnected();

    protected abstract void onConnectFailed(BluetoothGatt bluetoothGatt,
                                            BluetoothDevice bluetoothDevice);

    private class BluetoothStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                               BluetoothAdapter.ERROR);

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
        AndPermission.with(mContext)
                     .runtime()
                     .permission(Permission.Group.LOCATION)
                     .onGranted(new Action<List<String>>() {
                         @Override
                         public void onAction(List<String> data) {
                             if (android.os.Build.VERSION.SDK_INT >= android.os.Build
                                     .VERSION_CODES.LOLLIPOP) {
                                 scanLollipop();

                             } else {
                                 scan();
                             }
                         }
                     })
                     .onDenied(new Action<List<String>>() {
                         @Override
                         public void onAction(List<String> data) {
                             permissionDenied();
                         }
                     })
                     .start();
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
        mHandler.sendEmptyMessageDelayed(MSG_BLE_LOLLIPOP_SCAN_STOP, scanTimeout());
    }

    private void scan() {
        mBluetoothAdapter.startLeScan(mLeCallback);
        onScanStarted();

        mHandler.removeMessages(MSG_BLE_SCAN_STOP);
        mHandler.sendEmptyMessageDelayed(MSG_BLE_SCAN_STOP, scanTimeout());
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

                onScannedPeripheral(device, rssi, deviceName, manufacturerData);

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

    private BluetoothAdapter.LeScanCallback mLeCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            BleScanRecord record = BleScanRecord.parseFromBytes(scanRecord);

            if (record != null) {
                String deviceName = record.getDeviceName();

                byte[] manufacturerData = record.getManufacturerSpecificData(manufacturerId());

                onScannedPeripheral(device, rssi, deviceName, manufacturerData);
            }
        }
    };

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            bluetoothGatt = gatt;

            if (newState == BluetoothProfile.STATE_CONNECTED) {









            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){

            }





        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        }
    };

    private synchronized void disconnectGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    private synchronized void refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null && bluetoothGatt != null) {
                boolean success = (Boolean) refresh.invoke(bluetoothGatt);
                Log.i(TAG, "refreshDeviceCache, is success:  " + success);
            }
        } catch (Exception e) {
            Log.w(TAG, "refreshDeviceCache: ", e);
            e.printStackTrace();
        }
    }

    private synchronized void closeBluetoothGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

    private synchronized void stop() {

    }

    private void openBTByUser() {
        if (!mBluetoothAdapter.isEnabled()) {
            permissionDenied();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(enableBtIntent);
        }
    }

    private void permissionDenied() {
        Toast.makeText(mContext, BLE.BT_NO_PERMISSION, Toast.LENGTH_LONG)
             .show();
    }
}
