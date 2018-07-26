package com.suhen.android.libble.peripheral;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;

import com.suhen.android.libble.utils.ClsUtils;

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
    private static BlePeripheral sBlePeripheral;

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BlePeripheral(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized BlePeripheral getInstance(Context context) {
        if (sBlePeripheral == null) {
            sBlePeripheral = new BlePeripheral(context);
        }
        return sBlePeripheral;
    }

    /* init system service */

    /**
     * open BT, set device is discoverable
     */
    public void initBle() {
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            throw new RuntimeException("Your device is not support BLE.");
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothAdapter.enable();



        String localMacAddress = ClsUtils.getLocalMacAddress();



    }

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

}
