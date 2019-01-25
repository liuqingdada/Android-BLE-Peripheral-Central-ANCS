package com.suhen.android.libble.peripheral;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.peripheral.base.BleBasePeripheral;
import com.suhen.android.libble.peripheral.callback.BasePeripheralCallback;
import com.suhen.android.libble.utils.ClsUtils;
import com.suhen.android.libble.utils.StringUtil;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 * <p>
 * This is BLE Peripheral simple encapsulation, if you need more character, just override.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class BlePeripheral extends BleBasePeripheral implements IPeripheral {
    private static final String TAG = BlePeripheral.class.getSimpleName();

    private HandlerThread mGattServerCallbackThread;
    private Handler mGattServerCallbackHandler;
    protected Queue<BasePeripheralCallback> mBasePeripheralCallbacks = new ConcurrentLinkedQueue<>();

    private HandlerThread mGattServerReadWriteThread;
    private Handler mGattServerReadWriteHandler;

    protected Context mContext;
    private Lock mLock = new ReentrantLock();

    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothLeAdvertiser mLeAdvertiser;
    protected BluetoothGattServer mBluetoothGattServer;

    private BluetoothDevice mConnectionDevice;
    private BluetoothStatusReceiver mBluetoothStatusReceiver;

    protected BlePeripheral(Context context) {
        mContext = context;
    }

    @Override
    public void onCreate() {
        mGattServerCallbackThread = new HandlerThread("gatt-server-callback-looper-thread");
        mGattServerCallbackThread.start();
        mGattServerCallbackHandler = new Handler(mGattServerCallbackThread.getLooper());

        mGattServerReadWriteThread = new HandlerThread("gatt-server-read-write-looper-thread");
        mGattServerReadWriteThread.start();
        mGattServerReadWriteHandler = new Handler(mGattServerReadWriteThread.getLooper());

        mBluetoothStatusReceiver = new BluetoothStatusReceiver();
        IntentFilter bleIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothStatusReceiver, bleIntentFilter);
    }

    @Override
    public void addBasePeripheralCallback(BasePeripheralCallback basePeripheralCallback) {
        mLock.lock();
        try {
            boolean isContains = false;
            for (BasePeripheralCallback peripheralCallback : mBasePeripheralCallbacks) {
                if (peripheralCallback.getParentUuid().equalsIgnoreCase(basePeripheralCallback.getParentUuid()) &&
                        peripheralCallback.getChildUuid().equalsIgnoreCase(basePeripheralCallback.getChildUuid())) {
                    isContains = true;
                }
            }
            if (!isContains) {
                mBasePeripheralCallbacks.add(basePeripheralCallback);
            }
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void setup() {
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(
                Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.d(TAG, "setup: " + BLE.NOT_SUPPORT_PERIPHERAL);
            mGattServerCallbackHandler.post(this::peripheralNotSupport);
            return;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.d(TAG, "setup: BluetoothAdapter is null, " + BLE.NOT_SUPPORT_PERIPHERAL);
            mGattServerCallbackHandler.post(this::peripheralNotSupport);
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) { // if bt not enable

            if (!mBluetoothAdapter.enable()) { // if bt open fail
                openBTByUser();
            }

        } else {
            mGattServerCallbackHandler.post(this::start);
        }
    }

    @Override
    public void preparePair() {
        ClsUtils.setDiscoverableTimeout(120);
    }

    @Override
    public boolean isConnected() {
        if (mConnectionDevice == null) {
            return false;
        }
        return mBluetoothManager.getConnectionState(mConnectionDevice, BluetoothProfile.GATT_SERVER) == BluetoothProfile.STATE_CONNECTED;
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mBluetoothStatusReceiver);
        stop();
        mGattServerCallbackThread.quitSafely();
        mGattServerReadWriteThread.quitSafely();
    }

    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */
    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */
    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */

    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            switch (newState) {

                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "onConnectionStateChange: peripheral STATE_CONNECTED");
                    mConnectionDevice = device;
                    // create bond
                    if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        try {
                            device.setPairingConfirmation(true);
                        } catch (final SecurityException e) {
                            Log.e(TAG, "onConnectionStateChange: ", e);
                        }
                        device.createBond();
                    } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        mGattServerCallbackHandler.post(() -> {
                            if (mBluetoothGattServer != null) {
                                mBluetoothGattServer.connect(device, true);
                            }
                        });
                    }

                    mGattServerCallbackHandler.post(() -> onConnected(device)); // central incomming
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d(TAG, "onConnectionStateChange: peripheral STATE_DISCONNECTED");

                    mGattServerCallbackHandler.post(() -> onDisconnected(device)); // central outgoing
                    break;
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    if (basePeripheralCallback.getParentUuid().equalsIgnoreCase(characteristic.getService().getUuid().toString()) &&
                            basePeripheralCallback.getChildUuid().equalsIgnoreCase(characteristic.getUuid().toString())) {
                        basePeripheralCallback.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                    }
                }
            });
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    if (basePeripheralCallback.getParentUuid().equalsIgnoreCase(characteristic.getService().getUuid().toString()) &&
                            basePeripheralCallback.getChildUuid().equalsIgnoreCase(characteristic.getUuid().toString())) {
                        basePeripheralCallback.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                    }
                }
            });
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    if (basePeripheralCallback.getParentUuid().equalsIgnoreCase(descriptor.getCharacteristic().getUuid().toString()) &&
                            basePeripheralCallback.getChildUuid().equalsIgnoreCase(descriptor.getUuid().toString())) {
                        basePeripheralCallback.onDescriptorReadRequest(device, requestId, offset, descriptor);
                    }
                }
            });
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    if (basePeripheralCallback.getParentUuid().equalsIgnoreCase(descriptor.getCharacteristic().getUuid().toString()) &&
                            basePeripheralCallback.getChildUuid().equalsIgnoreCase(descriptor.getUuid().toString())) {
                        basePeripheralCallback.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                    }
                }
            });
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            mGattServerReadWriteHandler.post(() -> mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null));
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            mGattServerReadWriteHandler.post(() -> {
                if (currentIndicationCharacteristic == null || !lockCurrentIndication.get()) {
                    return;
                }
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    if (basePeripheralCallback.getParentUuid().equalsIgnoreCase(currentIndicationCharacteristic.getService().getUuid().toString()) &&
                            basePeripheralCallback.getChildUuid().equalsIgnoreCase(currentIndicationCharacteristic.getUuid().toString())) {
                        basePeripheralCallback.onNotificationSent(device, currentIndicationCharacteristic, status);
                    }
                }
            });
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            mGattServerReadWriteHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    basePeripheralCallback.onMtuChanged(device, mtu);
                }
            });
        }

        @Override
        public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            mGattServerReadWriteHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    basePeripheralCallback.onPhyUpdate(device, txPhy, rxPhy, status);
                }
            });
        }

        @Override
        public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            mGattServerReadWriteHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    basePeripheralCallback.onPhyRead(device, txPhy, rxPhy, status);
                }
            });
        }
    };

    /* BluetoothGattServerCallback END */       /* BluetoothGattServerCallback END */
    /* BluetoothGattServerCallback END */       /* BluetoothGattServerCallback END */
    /* BluetoothGattServerCallback END */       /* BluetoothGattServerCallback END */

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
                        mGattServerCallbackHandler.post(BlePeripheral.this::start);

                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.e(TAG, "bluetooth TURNING_ON");
                        break;
                }
            }
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED) {
                    final BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    // successfully bonded
                    context.unregisterReceiver(this);

                    mGattServerCallbackHandler.post(() -> {
                        if (mBluetoothGattServer != null && bondedDevice.equals(mConnectionDevice)) {
                            mBluetoothGattServer.connect(bondedDevice, true);
                        }
                    });
                    Log.e(TAG, "successfully bonded");
                }
            }
        }
    }

    // Avoid multiple initialization
    private AtomicBoolean peripheralFlag = new AtomicBoolean(false);

    /**
     * on bluetooth is open, or it is already open.
     */
    private synchronized void start() {
        if (peripheralFlag.get()) {
            Log.w(TAG, "start: peripheral is already initialized.");
            return;
        }

        try {
            // step 1
            String btAddress = ClsUtils.getBtAddressViaReflection();
            StringUtil.putString(mContext, BLE.PERIPHERAL_MAC, btAddress);

            // step 2
            //preparePair();

            // step 3
            String peripheralName = generatePeripheralName();
            mBluetoothAdapter.setName(peripheralName);
            Log.d(TAG, "start: " + peripheralName);
            StringUtil.putString(mContext, BLE.PERIPHERAL_NAME, peripheralName);

            // step 4
            if (!openGattServer()) {
                Log.d(TAG, "start: openGattServer error, " + BLE.NOT_SUPPORT_PERIPHERAL);
                mGattServerCallbackHandler.post(this::peripheralNotSupport);
                return;
            }
            addGattService();

            // step 5
            startAdvertising();

        } catch (Exception e) {
            Log.w(TAG, "start: ", e);
        } finally {
            if (mBluetoothAdapter.isEnabled()) {
                peripheralFlag.set(true);
            }
        }
    }

    private synchronized void stop() {
        try {
            stopAdvertising();
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
        } catch (Exception e) {
            Log.w(TAG, "stop: ", e);
        } finally {
            peripheralFlag.set(false);
        }
    }

    private void openBTByUser() {
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(mContext, BLE.BT_NO_PERMISSION, Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(enableBtIntent);
        }
    }

    /* GATT */
    private boolean openGattServer() {
        mLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mLeAdvertiser == null) {
            return false;
        }

        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mBluetoothGattServerCallback);
        mBluetoothGattServer.clearServices();

        return true;
    }

    protected void startAdvertising() {
        if (mLeAdvertiser != null) {
            AdvertiseData advertiseData = generateAdvertiseData();

            Log.i(TAG, "startAdvertising: " + advertiseData.toString());

            mLeAdvertiser.startAdvertising(generateAdvertiseSettings(), advertiseData, mAdvertiseCallback);
        }
    }

    protected void stopAdvertising() {
        if (mLeAdvertiser != null) {
            mLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "onStartSuccess: " + settingsInEffect.toString());

            mGattServerCallbackHandler.post(() -> onPeripheralStartSuccess(settingsInEffect));
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.w(TAG, "onStartFailure: " + errorCode);


            mGattServerCallbackHandler.post(() -> onPeripheralStartFailure(errorCode));
        }
    };

    /**
     * @param status BluetoothGatt.GATT_SUCCESS
     */
    @Override
    public void sendResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value) {
        mGattServerReadWriteHandler.post(() -> {
            if (!isConnected()) {
                return;
            }
            mBluetoothGattServer.sendResponse(device, requestId, status, offset, value);
        });
    }

    @Override
    public void notify(BluetoothGattCharacteristic characteristic) {
        mGattServerReadWriteHandler.post(() -> {
            if (!isConnected()) {
                return;
            }
            mBluetoothGattServer.notifyCharacteristicChanged(mConnectionDevice, characteristic, false);
        });
    }

    private BluetoothGattCharacteristic currentIndicationCharacteristic;
    private AtomicBoolean lockCurrentIndication = new AtomicBoolean(false);

    @Override
    public boolean lockCurrentIndication(BluetoothGattCharacteristic indicationCharacteristic) {
        if (lockCurrentIndication.get()) {
            return false; // 已经被占用
        } else {
            lockCurrentIndication.set(true);
            currentIndicationCharacteristic = indicationCharacteristic;
            return true; // 锁成功，此时不允许其他特征 Indication
        }
    }

    @Override
    public void unlockCurrentIndication() {
        if (lockCurrentIndication.get()) {
            lockCurrentIndication.set(false);
            currentIndicationCharacteristic = null;
        }
    }

    @Override
    public boolean indicate(byte[] value) {
        if (!isConnected() || currentIndicationCharacteristic == null) {
            return false;
        }
        currentIndicationCharacteristic.setValue(value);
        return mBluetoothGattServer.notifyCharacteristicChanged(mConnectionDevice, currentIndicationCharacteristic, true);
    }
}
