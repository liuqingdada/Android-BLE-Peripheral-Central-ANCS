package com.suhen.android.libble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import com.suhen.android.libble.peripheral.BlePeripheral;
import com.suhen.android.libble.peripheral.IPeripheral;

import java.lang.reflect.Constructor;

/**
 * Created by suhen
 * 18-7-26.
 * Email: 1239604859@qq.com
 * <p>
 * Generally, peripheral and central devices will only have an instance,
 * <p>
 * <p>
 */
public final class BLE {
    /**
     * When peripheral start advertising, use {@link SharedPreferences#getString(String, String)}
     * to get bluetooth address.
     */
    public static final String PERIPHERAL_MAC = "BT_MAC_ADDRESS";

    /**
     * When peripheral start advertising, use {@link SharedPreferences#getString(String, String)}
     * to get peripheral name, show this name to user, and central can use this name to connect.
     */
    public static final String PERIPHERAL_NAME = "PERIPHERAL_NAME";

    public static final String BT_NO_PERMISSION = "Unable to get bluetooth permission";
    public static final String NOT_SUPPORT_PERIPHERAL = "This device is not support ble peripheral";

    /**
     * Create a new peripheral instance.
     */
    public synchronized static IPeripheral newPeripheral(
            Class<? extends BlePeripheral> clazz,
            Context context
    ) throws Exception {
        Constructor<? extends BlePeripheral> constructor =
                clazz.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        return constructor.newInstance(context);
    }

    /**
     * is support peripheral or central
     */
    public static boolean isSupportBle(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            BluetoothManager bluetoothManager =
                    (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                return false;
            }

            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

            return bluetoothAdapter != null;
        } else {
            return false;
        }
    }
}
