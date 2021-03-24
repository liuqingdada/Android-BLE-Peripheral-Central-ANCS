package com.android.lib.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import com.android.lib.ble.central.ICentral;
import com.android.lib.ble.peripheral.IPeripheral;

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
    public synchronized static IPeripheral newPeripheral(Class<? extends IPeripheral> clazz) throws Exception {
        Constructor<? extends IPeripheral> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    public synchronized static ICentral newCentral(
            Class<? extends ICentral> clazz,
            BluetoothDevice device
    ) throws Exception {
        Constructor<? extends ICentral> constructor =
                clazz.getDeclaredConstructor(BluetoothDevice.class);
        constructor.setAccessible(true);
        return constructor.newInstance(device);
    }

    /**
     * 中心设备只要判断这个即可
     */
    public static boolean isSupportCentral(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    /**
     * 并不是 Android L 的系统就可以支持 BLE Peripheral, 这个和硬件也有关系
     * 最终以组件 Callback 为准
     */
    public static boolean isSupportPeripheral(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
