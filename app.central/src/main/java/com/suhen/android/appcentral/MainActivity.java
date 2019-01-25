package com.suhen.android.appcentral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.suhen.android.appcentral.service.SimpleBleCentralService;
import com.suhen.android.libble.BLE;
import com.suhen.android.libble.central.callback.CentralStatusCallback;
import com.suhen.android.libble.central.sdk.BleScanRecord;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private AppCompatEditText inputView;
    private TextView statusView;
    private String mScanMac;


    private Handler mMainHandler = new Handler();
    private SimpleBleCentralService mBleCentralService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputView = findViewById(R.id.ed_input_mac);
        statusView = findViewById(R.id.textView_status);

        if (BLE.isSupportBle(this)) {
            Intent intent = new Intent(this, SimpleBleCentralService.class);
            ContextCompat.startForegroundService(this, intent);
            bindService(intent, mBleCentralServiceConnection, Context.BIND_AUTO_CREATE);

            findViewById(R.id.bt_rescan).setOnClickListener(v -> {
                if (inputView.getText() == null) {
                    return;
                }
                String inputMac = inputView.getText().toString();

                if (BluetoothAdapter.checkBluetoothAddress(inputMac)) {
                    mScanMac = inputMac;
                    mBleCentralService.getCentral().disconnect();
                    mBleCentralService.getCentral().stopScan();
                    mBleCentralService.getCentral().setup();
                } else {
                    Toast.makeText(v.getContext(), "输入蓝牙mac地址无效", Toast.LENGTH_SHORT).show();
                }
            });
        }
        findViewById(R.id.button_clear_log).setOnClickListener(v -> statusView.setText(""));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private ServiceConnection mBleCentralServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SimpleBleCentralService.BleCentralBinder bleCentralBinder = (SimpleBleCentralService.BleCentralBinder) service;
            mBleCentralService = bleCentralBinder.getBleService();
            mBleCentralService.getCentral().setCentralStatusCallback(mCentralStatusCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleCentralService = null;
        }
    };

    private CentralStatusCallback mCentralStatusCallback = new CentralStatusCallback() {
        @Override
        public void onScanStarted() {
            Log.i(TAG, "onScanStarted: " + Thread.currentThread().getName());
            mMainHandler.post(() -> statusView.append("onScanStarted\n\n"));
        }

        @Override
        public void onScanFinished() {
            mMainHandler.post(() -> statusView.append("onScanFinished\n\n"));
        }

        @Override
        public void onScannedPeripheral(ScanResult result, BleScanRecord bleScanRecord, BluetoothDevice remoteDevice, int rssi) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && result != null) {
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord != null) {
                    if (remoteDevice.getAddress().equalsIgnoreCase(mScanMac) || result.getDevice().getAddress().equalsIgnoreCase(mScanMac)) {
                        mBleCentralService.getCentral().stopScan();
                        mBleCentralService.getCentral().connect(remoteDevice, false, TRANSPORT_AUTO, PHY_LE_1M_MASK);
                    }
                }
                mMainHandler.post(() -> statusView.append(result.toString() + "\n\n"));
            }
        }

        @Override
        public void onConnectStarted(BluetoothGatt bluetoothGatt) {
            mMainHandler.post(() -> statusView.append("onConnectStarted\n\n"));
        }

        @Override
        public void onConnected(BluetoothGatt bluetoothGatt, int status) {
            StringBuilder stringBuilder = new StringBuilder();
            for (BluetoothGattService service : bluetoothGatt.getServices()) {
                stringBuilder.append(service.getUuid().toString()).append("\n");

                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    stringBuilder.append("\t")
                            .append(characteristic.getUuid())
                            .append(", prop: ")
                            .append(mBleCentralService.getCentral().getCharacteristicProperty(characteristic))
                            .append("\n");

                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                        stringBuilder.append("\t\t").append(descriptor.getUuid().toString()).append("\n");
                    }
                }
            }

            mMainHandler.post(() -> {
                statusView.append("onConnected\n");
                statusView.append(stringBuilder.toString());
                statusView.append("\n\n");
            });
        }

        @Override
        public void onConnectFailed(BluetoothGatt bluetoothGatt, int status) {
            mMainHandler.post(() -> statusView.append("onConnectFailed\n\n"));
        }

        @Override
        public void onDisconnected(BluetoothGatt bluetoothGatt) {
            mMainHandler.post(() -> statusView.append("onDisconnected\n\n"));
        }
    };
}
