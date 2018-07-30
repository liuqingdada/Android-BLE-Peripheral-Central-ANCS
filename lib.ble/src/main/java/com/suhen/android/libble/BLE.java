package com.suhen.android.libble;

import android.content.Context;

import com.suhen.android.libble.central.ICentral;
import com.suhen.android.libble.peripheral.BlePeripheral;
import com.suhen.android.libble.peripheral.IPeripheral;

import net.vidageek.mirror.dsl.Mirror;

import java.lang.reflect.Constructor;

/**
 * Created by suhen
 * 18-7-26.
 * Email: 1239604859@qq.com
 */
public class BLE {
    public static final String BT_MAC = "bt_mac_address";

    public static final String BT_NO_PERMISSION = "Unable to get bluetooth permission";

    // UUID
    public static final String SERVICE_UUID = "4622c045-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_INDICATE_UUID = "4622c046-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_WRITE_UUID = "4622c047-1cd2-4211-adc5-89df72c789ec";
    public static final String CHAR_READ_UUID = "4622c048-1cd2-4211-adc5-89df72c789ec";

    public static final int MANUFACTURER_ID = 0x01;

    /**
     * 主入口
     */
    public static IPeripheral peripheral(Class<? extends BlePeripheral> clazz,
                                         Context context) throws Exception {
        Constructor<? extends BlePeripheral> constructor = new Mirror().on(clazz)
                                                                       .reflect()
                                                                       .constructor()
                                                                       .withArgs(Context.class);
        constructor.setAccessible(true);

        return constructor.newInstance(context);
    }

    public static ICentral central() {
        return null;
    }
}
