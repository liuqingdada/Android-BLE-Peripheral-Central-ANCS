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
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.utils.ClsUtils;
import com.suhen.android.libble.utils.StringUtil;
import com.suhen.android.libble.utils.TypeConversion;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 * <p>
 * This is BLE Peripheral simple encapsulation, if you need more character, just override.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BlePeripheral extends BluetoothGattServerCallback {
    private static final String TAG = BlePeripheral.class.getSimpleName();

    private static BlePeripheral sBlePeripheral;
    private BluetoothStatusReceiver mBluetoothStatusReceiver = new BluetoothStatusReceiver();

    protected Context mContext;

    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothLeAdvertiser mLeAdvertiser;
    protected BluetoothGattServer mBluetoothGattServer;

    protected boolean isConnected;

    protected BlePeripheral(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized BlePeripheral getInstance(Context context) {
        if (sBlePeripheral == null) {
            sBlePeripheral = new BlePeripheral(context);
        }
        return sBlePeripheral;
    }

    public void onCreate() {
        IntentFilter bleIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothStatusReceiver, bleIntentFilter);
    }

    /**
     * step 1.
     * Call this method to confirm; and it can initialize ble.
     */
    public boolean isSupportBle() {
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

    /**
     * step 2.
     * init peripheral
     */
    public void setupPeripheral() {
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(
                Context.BLUETOOTH_SERVICE);
        assert mBluetoothManager != null;
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (!mBluetoothAdapter.isEnabled()) { // if bt not enable

            if (!mBluetoothAdapter.enable()) { // if bt open fail
                openBTByUser();
            }

        } else {
            main();
        }
    }

    public void preparePair() {
        mBluetoothAdapter.startDiscovery();
        ClsUtils.setDiscoverableTimeout(100);
    }

    public void onDestroy() {
        mContext.unregisterReceiver(mBluetoothStatusReceiver);
    }

    protected void onPeripheralStartSuccess(AdvertiseSettings settingsInEffect) {
    }

    protected void onPeripheralStartFailure(int errorCode) {
        stopAdvertising();
    }

    protected void onConnected() {
    }

    protected void onDisconnected() {
        stopAdvertising();
        startAdvertising();
    }

    protected void onReceiveBytes(byte[] bytes) {
    }

    protected void sendBleBytes(byte[] bytes) {
    }

    /**
     * Note that some devices do not support long names.
     * Recommended within 12 bytes.
     */
    protected String generatePeripheralName() {
        String mac = StringUtil.getString(mContext, BLE.BT_MAC, "");

        mac = mac.substring(mac.length() - 7, mac.length());
        mac = mac.replaceAll(":", "");

        return "Suhen_" + mac;
    }

    /**
     * @return Your BLE peripheral settings.
     */
    protected AdvertiseSettings generateAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTimeout(0)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();
    }

    /**
     * Recommended within 8 bytes.
     */
    protected AdvertiseData generateAdvertiseData() {
        String mac = StringUtil.getString(mContext, BLE.BT_MAC, "00:00:00:00:00:00");
        mac = mac.replaceAll(":", "");
        byte[] mac_bytes = TypeConversion.hexString2Bytes(mac);
        Log.d(TAG, "generateAdvertiseData: " + Arrays.toString(mac_bytes));

        return new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addManufacturerData(BLE.MANUFACTURER_ID, mac_bytes)
                //.addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID)))
                .build();
    }

    /*  prepare gatt service and characteristic */

    /**
     * All params must be standard UUID, you can use {@link java.util.UUID} generate this param,
     * or manually generated UUID.
     * <p>
     * If you use illegal UUID, peripheral will be open failure.
     * <p>
     * Note that this is the simplest implementation
     */
    protected void addGattService() {
        BluetoothGattService peripheralService = new BluetoothGattService(
                UUID.fromString(BLE.SERVICE_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic characteristicIndicate = new BluetoothGattCharacteristic(
                UUID.fromString(BLE.CHAR_INDICATE_UUID),
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(
                UUID.fromString(BLE.CHAR_WRITE_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        peripheralService.addCharacteristic(characteristicWrite);
        peripheralService.addCharacteristic(characteristicIndicate);

        mBluetoothGattServer.addService(peripheralService);
    }

    /*  prepare gatt service and characteristic end */

    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */
    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */
    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        super.onConnectionStateChange(device, status, newState);
        switch (newState) {

            case BluetoothProfile.STATE_CONNECTED:
                isConnected = true;
                Log.d(TAG, "onConnectionStateChange: peripheral STATE_CONNECTED");

                onConnected(); // central incomming
                break;

            case BluetoothProfile.STATE_DISCONNECTED:
                isConnected = false;
                Log.d(TAG, "onConnectionStateChange: peripheral STATE_DISCONNECTED");

                onDisconnected(); // central outgoing
                break;
        }
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        super.onServiceAdded(status, service);
        Log.v(TAG, "onServiceAdded: status: " + status + ", service: " + service.getUuid());
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            Log.v(TAG, "onServiceAdded: service has characteristic: " + characteristic.getUuid());
        }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                                           responseNeeded, offset, value);
        if (characteristic.getUuid()
                          .toString()
                          .equals(BLE.CHAR_WRITE_UUID)) {
            // receive data:
            Log.i(TAG, "onCharacteristicWriteRequest: " + Arrays.toString(value));
            onReceiveBytes(value);
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                                              value);
        }
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                        BluetoothGattDescriptor descriptor) {
        super.onDescriptorReadRequest(device, requestId, offset, descriptor);
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                         BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite,
                                         boolean responseNeeded, int offset, byte[] value) {
        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite,
                                       responseNeeded,
                                       offset, value);
    }

    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        super.onExecuteWrite(device, requestId, execute);
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
        super.onNotificationSent(device, status);
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged(device, mtu);
    }

    @Override
    public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(device, txPhy, rxPhy, status);
    }

    @Override
    public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyRead(device, txPhy, rxPhy, status);
    }

    /* BluetoothGattServerCallback END */       /* BluetoothGattServerCallback END */
    /* BluetoothGattServerCallback END */       /* BluetoothGattServerCallback END */
    /* BluetoothGattServerCallback END */       /* BluetoothGattServerCallback END */

    private class BluetoothStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                               BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        isConnected = false;
                        Log.e(TAG, "bluetooth OFF");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.e(TAG, "bluetooth TURNING_OFF");
                        end();
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.e(TAG, "bluetooth ON");
                        main();

                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.e(TAG, "bluetooth TURNING_ON");
                        break;
                }
            }
        }
    }

    /**
     * on bluetooth is open, or it is already open.
     */
    private void main() {
        // step 1
        String btAddress = ClsUtils.getBtAddressViaReflection();
        StringUtil.putString(mContext, BLE.BT_MAC, btAddress);

        // step 2
        preparePair();

        // step 3
        String bluetoothName = generatePeripheralName();
        Log.d(TAG, "main: " + bluetoothName);
        mBluetoothAdapter.setName(bluetoothName);

        // step 4
        openGattServer();
        addGattService();

        // step 5
        startAdvertising();
    }

    private void end() {
        stopAdvertising();
        mBluetoothGattServer.clearServices();
        mBluetoothGattServer.close();
    }

    private void openBTByUser() {
        Toast.makeText(mContext, BLE.BT_NO_PERMISSION, Toast.LENGTH_LONG)
             .show();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(enableBtIntent);
        }
    }

    /* GATT */
    private void openGattServer() {
        mLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, this);
        mBluetoothGattServer.clearServices();

    }

    private void startAdvertising() {
        mLeAdvertiser.startAdvertising(generateAdvertiseSettings(),
                                       generateAdvertiseData(), mAdvertiseCallback);
    }

    private void stopAdvertising() {
        mLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "onStartSuccess: " + settingsInEffect.toString());
            onPeripheralStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.w(TAG, "onStartFailure: " + errorCode);
            onPeripheralStartFailure(errorCode);
        }
    };
}
