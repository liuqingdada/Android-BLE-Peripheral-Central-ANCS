package com.suhen.android.libble.peripheral;

import com.suhen.android.libble.peripheral.callback.BasePeripheralCallback;

/**
 * Created by liuqing
 * 2018/7/30.
 * Email: suhen0420@163.com
 */
public interface IPeripheral {
    void onCreate();

    void addBasePeripheralCallback(BasePeripheralCallback basePeripheralCallback);

    void setup();

    void preparePair();

    boolean isConnected();

    void onDestroy();
}
