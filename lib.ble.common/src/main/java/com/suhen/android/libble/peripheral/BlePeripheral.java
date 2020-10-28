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
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.widget.Toast;

import com.android.common.utils.AppUtils;
import com.android.common.utils.LogUtil;
import com.android.common.utils.SharedPrefsUtils;
import com.suhen.android.libble.BLE;
import com.suhen.android.libble.message.BleMessage;
import com.suhen.android.libble.nrfscan.FastPairConstant;
import com.suhen.android.libble.peripheral.base.BleBasePeripheral;
import com.suhen.android.libble.peripheral.base.IndicateRunnable;
import com.suhen.android.libble.peripheral.base.SerialExecutor;
import com.suhen.android.libble.peripheral.callback.BasePeripheralCallback;
import com.suhen.android.libble.peripheral.callback.BluetoothCallback;
import com.suhen.android.libble.utils.ClsUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final String TAG = "BlePeripheral";
    private static final int ACCESS_BLUETOOTH_LE_ADVERTISER_INTERVAL = 10;
    private static final int ACCESS_BLUETOOTH_LE_ADVERTISER_MAX_TIMES = 900;

    private HandlerThread mGattServerCallbackThread;
    private Handler mGattServerCallbackHandler;
    protected Queue<BasePeripheralCallback> mBasePeripheralCallbacks =
            new ConcurrentLinkedQueue<>();

    private HandlerThread mGattServerWriteThread;
    private Handler mGattServerWriteHandler;

    private final SerialExecutor mIndicateService = new SerialExecutor();
    /**
     * key: serviceUUID + characterUUID
     */
    private final Map<String, IndicateRunnable> indicateRunnables = new ConcurrentHashMap<>();

    protected Context mContext = AppUtils.application;
    private final Lock mLock = new ReentrantLock();
    private String bpKey = "";

    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothLeAdvertiser mLeAdvertiser;
    protected volatile BluetoothGattServer mBluetoothGattServer;

    private BluetoothDevice mConnectionDevice;
    private BluetoothStatusReceiver mBluetoothStatusReceiver;
    private BluetoothCallback bluetoothCallback;

    protected BlePeripheral() {
        AssetManager assets = mContext.getAssets();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(assets.open("ble/bp_key.txt")));
            bpKey = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                    assets.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreate() {
        LogUtil.d(TAG, "onCreate: " + bpKey);

        mGattServerCallbackThread = new HandlerThread("gatt-server-callback-looper-thread");
        mGattServerCallbackThread.start();
        mGattServerCallbackHandler = new Handler(mGattServerCallbackThread.getLooper());

        mGattServerWriteThread = new HandlerThread("gatt-server-write-looper-thread");
        mGattServerWriteThread.start();
        mGattServerWriteHandler = new Handler(mGattServerWriteThread.getLooper());

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
                if (peripheralCallback.getParentUuid()
                        .equalsIgnoreCase(basePeripheralCallback.getParentUuid()) &&
                        peripheralCallback.getChildUuid()
                                .equalsIgnoreCase(basePeripheralCallback.getChildUuid())) {
                    isContains = true;
                    break;
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
            LogUtil.d(TAG, "setup: " + BLE.NOT_SUPPORT_PERIPHERAL);
            mGattServerCallbackHandler.post(this::peripheralNotSupport);
            return;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            LogUtil.d(TAG, "setup: BluetoothAdapter is null, " + BLE.NOT_SUPPORT_PERIPHERAL);
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
        return mBluetoothManager.getConnectionState(
                mConnectionDevice,
                BluetoothProfile.GATT_SERVER
        ) == BluetoothProfile.STATE_CONNECTED;
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mBluetoothStatusReceiver);
        stop();
        mIndicateService.clear();
        mGattServerCallbackThread.quitSafely();
        mGattServerWriteThread.quitSafely();
    }

    @Override
    public void setBluetoothCallback(BluetoothCallback bluetoothCallback) {
        this.bluetoothCallback = bluetoothCallback;
    }

    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */
    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */
    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */

    private final BluetoothGattServerCallback gattServCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(
                BluetoothDevice device,
                int status,
                int newState
        ) {
            switch (newState) {

                case BluetoothProfile.STATE_CONNECTED:
                    LogUtil.d(TAG, "onConnectionStateChange: peripheral STATE_CONNECTED");
                    mConnectionDevice = device;
                    // create bond
                    /*if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        try {
                            device.setPairingConfirmation(true);
                        } catch (final SecurityException e) {
                            LogUtil.e(TAG, "onConnectionStateChange: ", e);
                        }
                        device.createBond();
                    } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        mGattServerCallbackHandler.post(() -> {
                            if (mBluetoothGattServer != null) {
                                mBluetoothGattServer.connect(device, false);
                            }
                        });
                    }*/

                    mGattServerCallbackHandler.post(() -> onConnected(device));
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    LogUtil.d(TAG, "onConnectionStateChange: peripheral STATE_DISCONNECTED");

                    mGattServerCallbackHandler.post(() -> {
                        indicateRunnables.clear();

                        onDisconnected(device);
                    });
                    break;
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
        }

        @Override
        public void onCharacteristicReadRequest(
                BluetoothDevice device,
                int requestId,
                int offset,
                BluetoothGattCharacteristic characteristic
        ) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    if (basePeripheralCallback.getParentUuid()
                            .equalsIgnoreCase(characteristic.getService().getUuid()
                                    .toString()) &&
                            basePeripheralCallback.getChildUuid()
                                    .equalsIgnoreCase(characteristic.getUuid()
                                            .toString())) {
                        basePeripheralCallback.onCharacteristicReadRequest(
                                device,
                                requestId,
                                offset,
                                characteristic
                        );
                    }
                }
                sendResponse(
                        device,
                        requestId,
                        offset,
                        characteristic.getValue()
                );
            });
        }

        @Override
        public void onCharacteristicWriteRequest(
                BluetoothDevice device,
                int requestId,
                BluetoothGattCharacteristic characteristic,
                boolean preparedWrite,
                boolean responseNeeded,
                int offset,
                byte[] value
        ) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    if (basePeripheralCallback.getParentUuid()
                            .equalsIgnoreCase(characteristic.getService().getUuid()
                                    .toString()) &&
                            basePeripheralCallback.getChildUuid()
                                    .equalsIgnoreCase(characteristic.getUuid()
                                            .toString())) {
                        basePeripheralCallback.onCharacteristicWriteRequest(
                                device,
                                requestId,
                                characteristic,
                                preparedWrite,
                                responseNeeded,
                                offset,
                                value
                        );
                    }
                }
                sendResponse(
                        device,
                        requestId,
                        offset,
                        value
                );
            });
        }

        @Override
        public void onDescriptorReadRequest(
                BluetoothDevice device,
                int requestId,
                int offset,
                BluetoothGattDescriptor descriptor
        ) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    if (basePeripheralCallback.getParentUuid()
                            .equalsIgnoreCase(descriptor.getCharacteristic().getUuid()
                                    .toString()) &&
                            basePeripheralCallback.getChildUuid()
                                    .equalsIgnoreCase(descriptor.getUuid().toString())) {
                        basePeripheralCallback.onDescriptorReadRequest(
                                device,
                                requestId,
                                offset,
                                descriptor
                        );
                    }
                }
                sendResponse(
                        device,
                        requestId,
                        offset,
                        descriptor.getValue()
                );
            });
        }

        @Override
        public void onDescriptorWriteRequest(
                BluetoothDevice device,
                int requestId,
                BluetoothGattDescriptor descriptor,
                boolean preparedWrite,
                boolean responseNeeded,
                int offset,
                byte[] value
        ) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    if (basePeripheralCallback.getParentUuid()
                            .equalsIgnoreCase(descriptor.getCharacteristic().getUuid()
                                    .toString()) &&
                            basePeripheralCallback.getChildUuid()
                                    .equalsIgnoreCase(descriptor.getUuid().toString())) {
                        basePeripheralCallback.onDescriptorWriteRequest(
                                device,
                                requestId,
                                descriptor,
                                preparedWrite,
                                responseNeeded,
                                offset,
                                value
                        );
                    }
                }
                sendResponse(
                        device,
                        requestId,
                        offset,
                        descriptor.getValue()
                );
            });
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            mGattServerCallbackHandler.post(() -> sendResponse(
                    device,
                    requestId,
                    0,
                    null
            ));
        }

        /**
         * 如果是indicate, 要等这个方法回调后才能发送下一个包
         */
        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            mGattServerCallbackHandler.post(() -> {
                LogUtil.d(TAG, "onNotificationSent: " + status);
                nextIndicatePackage();
            });
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    basePeripheralCallback.onMtuChanged(device, mtu);
                }
            });
        }

        @Override
        public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            mGattServerCallbackHandler.post(() -> {
                for (BasePeripheralCallback basePeripheralCallback : mBasePeripheralCallbacks) {
                    basePeripheralCallback.onPhyUpdate(device, txPhy, rxPhy, status);
                }
            });
        }

        @Override
        public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            mGattServerCallbackHandler.post(() -> {
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
                int state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        LogUtil.e(TAG, "bluetooth OFF");

                        if (bluetoothCallback != null) {
                            bluetoothCallback.onClose();
                        }
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        LogUtil.e(TAG, "bluetooth TURNING_OFF");
                        stop();
                        break;

                    case BluetoothAdapter.STATE_ON:
                        LogUtil.e(TAG, "bluetooth ON");
                        mGattServerCallbackHandler.post(BlePeripheral.this::start);

                        if (bluetoothCallback != null) {
                            bluetoothCallback.onOpen();
                        }
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        LogUtil.e(TAG, "bluetooth TURNING_ON");
                        break;
                }
            }
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                final int state =
                        intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED) {
                    final BluetoothDevice bondedDevice =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    // successfully bonded
                    context.unregisterReceiver(this);

                    mGattServerCallbackHandler.post(() -> {
                        if (mBluetoothGattServer != null && bondedDevice
                                .equals(mConnectionDevice)) {
                            mBluetoothGattServer.connect(bondedDevice, true);
                        }
                    });
                    LogUtil.e(TAG, "successfully bonded");
                }
            }
        }
    }

    // Avoid multiple initialization
    private final AtomicBoolean peripheralFlag = new AtomicBoolean(false);

    /**
     * on bluetooth is open, or it is already open.
     */
    private synchronized void start() {
        if (peripheralFlag.get()) {
            LogUtil.w(TAG, "start: peripheral is already initialized.");
            return;
        }

        try {
            // step 1
            String btAddress = ClsUtils.getBtAddress();
            LogUtil.d(TAG, "start: get mac address " + btAddress);
            SharedPrefsUtils.putString(
                    mContext,
                    FastPairConstant.Extra.SP_NAME,
                    BLE.PERIPHERAL_MAC,
                    btAddress
            );

            // step 2
            //preparePair();

            // step 3
            String peripheralName = generatePeripheralName();
            mBluetoothAdapter.setName(peripheralName);
            LogUtil.d(TAG, "start: " + peripheralName);
            SharedPrefsUtils.putString(
                    mContext,
                    FastPairConstant.Extra.SP_NAME,
                    BLE.PERIPHERAL_NAME,
                    peripheralName
            );

            // step 4
            if (!openGattServer()) {
                LogUtil.d(TAG, "start: openGattServer error, " + BLE.NOT_SUPPORT_PERIPHERAL);
                mGattServerCallbackHandler.post(this::peripheralNotSupport);
                return;
            }
            addGattService();

            // step 5
            startAdvertising();

        } catch (Exception e) {
            LogUtil.w(TAG, "start: ", e);
        } finally {
            if (mBluetoothAdapter.isEnabled()) {
                peripheralFlag.set(true);
            }
        }
    }

    private synchronized void stop() {
        try {
            indicateRunnables.clear();
            stopAdvertising();
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
        } catch (Exception e) {
            LogUtil.w(TAG, "stop: ", e);
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

        int openCount = ACCESS_BLUETOOTH_LE_ADVERTISER_MAX_TIMES;
        while (openCount-- > 0) {
            mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, gattServCallback);

            if (mBluetoothGattServer != null) {
                // open gatt server success
                break;
            }

            SystemClock.sleep(ACCESS_BLUETOOTH_LE_ADVERTISER_INTERVAL);
        }
        if (mBluetoothGattServer == null) {
            return false;
        } else {
            mBluetoothGattServer.clearServices();
            return true;
        }
    }

    protected void startAdvertising() {
        if (mLeAdvertiser != null) {
            AdvertiseData advertiseData = generateAdvertiseData();
            LogUtil.i(TAG, "startAdvertising advertiseData: " + advertiseData.toString());

            AdvertiseData scanResponse = generateAdvertiseResponse();
            LogUtil.i(TAG, "startAdvertising scanResponse: " + scanResponse.toString());

            mLeAdvertiser.startAdvertising(
                    generateAdvertiseSettings(),
                    advertiseData,
                    scanResponse,
                    mAdvertiseCallback
            );
        }
    }

    protected void stopAdvertising() {
        if (mLeAdvertiser != null) {
            mLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            LogUtil.i(TAG, "onStartSuccess: " + settingsInEffect.toString());

            mGattServerCallbackHandler.post(() -> onPeripheralStartSuccess(settingsInEffect));
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            LogUtil.w(TAG, "onStartFailure: " + errorCode);

            mGattServerCallbackHandler.post(() -> onPeripheralStartFailure(errorCode));
        }
    };

    @Override
    public void notify(String serviceUUID, String characterUUID, byte[] data) {
        if (mBluetoothGattServer != null) {
            BluetoothGattService service =
                    mBluetoothGattServer.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic =
                        service.getCharacteristic(UUID.fromString(characterUUID));
                notify(characteristic, data);
            }
        }
    }

    @Override
    public void notify(BluetoothGattCharacteristic characteristic, byte[] data) {
        mGattServerWriteHandler.post(() -> {
            if (!isConnected()) {
                return;
            }
            characteristic.setValue(data);
            mBluetoothGattServer.notifyCharacteristicChanged(
                    mConnectionDevice,
                    characteristic,
                    false
            );
        });
    }

    @Override
    public void indicate(String serviceUUID, String characterUUID, BleMessage message) {
        if (mBluetoothGattServer != null) {
            BluetoothGattService service =
                    mBluetoothGattServer.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic =
                        service.getCharacteristic(UUID.fromString(characterUUID));
                indicate(characteristic, message);
            }
        }
    }

    /**
     * 占用 {@link BluetoothGattServerCallback#onNotificationSent(BluetoothDevice, int)}
     * 将所有的 message put 到写线程中, 分包发, 等到响应后发下一个
     *
     * @param message 一条完整的消息, 需要分包发送
     */
    @Override
    public void indicate(BluetoothGattCharacteristic characteristic, BleMessage message) {
        mGattServerWriteHandler.post(() -> {
            if (!isConnected()) {
                return;
            }
            String key = characteristic.getService().getUuid().toString() +
                    characteristic.getUuid().toString();
            IndicateRunnable indicateRunnable = indicateRunnables.get(key);
            if (indicateRunnable == null) {
                indicateRunnable = new IndicateRunnable(
                        mBluetoothGattServer,
                        mConnectionDevice,
                        characteristic
                );
                indicateRunnables.put(key, indicateRunnable);
            }

            List<byte[]> subpackage = message.subpackage(message.getPayload());
            indicateRunnable.putSubPackage(subpackage);

            mIndicateService.execute(indicateRunnable);
        });
    }

    private void nextIndicatePackage() {
        Runnable active = mIndicateService.getActive();
        if (active instanceof IndicateRunnable) {
            IndicateRunnable indicateRunnable = (IndicateRunnable) active;
            indicateRunnable.next();
        }
    }

    private void sendResponse(
            BluetoothDevice device,
            int requestId,
            int offset,
            byte[] value
    ) {
        mBluetoothGattServer.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                value
        );
    }
}
