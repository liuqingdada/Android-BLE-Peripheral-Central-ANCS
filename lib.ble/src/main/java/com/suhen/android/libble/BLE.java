package com.suhen.android.libble;

import android.content.Context;
import android.content.SharedPreferences;

import com.suhen.android.libble.central.ICentral;
import com.suhen.android.libble.peripheral.BlePeripheral;
import com.suhen.android.libble.peripheral.IPeripheral;

import net.vidageek.mirror.dsl.Mirror;

import java.lang.reflect.Constructor;

/**
 * Created by suhen
 * 18-7-26.
 * Email: 1239604859@qq.com
 * <p>
 * Generally, peripheral and central devices will only have an instance,
 * because peripheral device can only be connected to a central equipment at the same time.
 * <p>
 * <p>
 * <p>
 * If you want to create multiple instances central devices,
 * please use {@link BLE#newCentral(Class, Context)}, peripheral in the same way.
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
    public static final String NOT_SUPPORT_CENTRAL = "This device is not support ble central";

    // UUID
    public static final String SERVICE_UUID = "4622c045-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_INDICATE_UUID = "4622c046-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_WRITE_UUID = "4622c047-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_READ_UUID = "4622c048-1cd2-4211-adc5-89df72c789ec";

    /**
     * To get the only one peripheral instance
     */
    public synchronized static IPeripheral getPeripheral(Class<? extends BlePeripheral> clazz,
                                                         Context context) throws Exception {
        if (PERIPHERAL == null) {
            Constructor<? extends BlePeripheral> constructor = new Mirror().on(clazz)
                                                                           .reflect()
                                                                           .constructor()
                                                                           .withArgs(Context.class);
            constructor.setAccessible(true);

            PERIPHERAL = constructor.newInstance(context);
        }

        return PERIPHERAL;
    }

    /**
     * To get the only one central instance
     */
    public synchronized static ICentral getCentral(Class<? extends ICentral> clazz,
                                                   Context context) throws Exception {
        if (CENTRAL == null) {
            Constructor<? extends ICentral> constructor = new Mirror().on(clazz)
                                                                      .reflect()
                                                                      .constructor()
                                                                      .withArgs(Context.class);
            constructor.setAccessible(true);

            CENTRAL = constructor.newInstance(context);
        }

        return CENTRAL;
    }

    private static volatile IPeripheral PERIPHERAL;

    private static volatile ICentral CENTRAL;

    /**
     * Create a new central instance.
     */
    public synchronized static ICentral newCentral(Class<? extends ICentral> clazz,
                                                   Context context) throws Exception {
        Constructor<? extends ICentral> constructor = new Mirror().on(clazz)
                                                                  .reflect()
                                                                  .constructor()
                                                                  .withArgs(Context.class);
        constructor.setAccessible(true);

        return constructor.newInstance(context);
    }

    /**
     * Create a new peripheral instance.
     */
    public synchronized static IPeripheral newPeripheral(Class<? extends BlePeripheral> clazz,
                                                         Context context) throws Exception {
        Constructor<? extends BlePeripheral> constructor = new Mirror().on(clazz)
                                                                       .reflect()
                                                                       .constructor()
                                                                       .withArgs(Context.class);
        constructor.setAccessible(true);

        return constructor.newInstance(context);
    }
}
