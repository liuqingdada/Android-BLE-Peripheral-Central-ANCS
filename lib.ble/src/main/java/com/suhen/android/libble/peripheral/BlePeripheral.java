package com.suhen.android.libble.peripheral;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.suhen.android.libble.utils.ClsUtils;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;

import java.util.List;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 * <p>
 * This is BLE Peripheral simple encapsulation, if you need more character, just override.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BlePeripheral {
    private static final String TAG = BlePeripheral.class.getSimpleName();

    private static BlePeripheral sBlePeripheral;
    private BluetoothStatusReceiver mBluetoothStatusReceiver = new BluetoothStatusReceiver();

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BlePeripheral(Context context) {
        mContext = context.getApplicationContext();

        IntentFilter bleIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothStatusReceiver, bleIntentFilter);
    }

    public static synchronized BlePeripheral getInstance(Context context) {
        if (sBlePeripheral == null) {
            sBlePeripheral = new BlePeripheral(context);
        }
        return sBlePeripheral;
    }

    /**
     * must call this method to confirm; and it can initialize ble.
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

    /* init system service */

    public void openBT() {
        AndPermission.with(mContext)
                     .runtime()
                     .permission(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
                     .onGranted(new Action<List<String>>() {
                         @Override
                         public void onAction(List<String> data) {
                             mBluetoothAdapter.enable();

                             String localMacAddress = ClsUtils.getLocalMacAddress();


                         }
                     })
                     .onDenied(new Action<List<String>>() {
                         @Override
                         public void onAction(List<String> data) {

                         }
                     })
                     .start();
    }

    public void openBT(Activity activity, int requestCode) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, requestCode);
        }
    }

    /*
    //蓝牙开启回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //判断requestCode是否为开启蓝牙时传进去的值，再做相应处理
        if(requestCode == 1){
            //蓝牙开启成功后的处理
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
     */

    /* init GATT */

    /*  prepare gatt service and characteristic */

    /**
     * all params must be standard UUID, you can use {@link java.util.UUID} generate this param,
     * or manually generated uuids.
     *
     * @param gattServiceUUID     standard UUID
     * @param characteristicUUIDs standard UUID
     */
    public void addGattService(String gattServiceUUID, List<String> characteristicUUIDs) {

    }




    /*  prepare gatt service and characteristic end */

    public void onDestroy() {
        mContext.unregisterReceiver(mBluetoothStatusReceiver);
    }

    private class BluetoothStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {

                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                               BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF: // 手机蓝牙关闭
                        Log.e(TAG, "手机蓝牙关闭");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF: // 手机蓝牙正在关闭
                        Log.e(TAG, "手机蓝牙正在关闭");
                        break;

                    case BluetoothAdapter.STATE_ON: // 手机蓝牙开启
                        Log.e(TAG, "手机蓝牙开启");
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON: // 手机蓝牙正在开启
                        Log.e(TAG, "手机蓝牙正在开启");
                        break;
                }
            }
        }
    }
}
