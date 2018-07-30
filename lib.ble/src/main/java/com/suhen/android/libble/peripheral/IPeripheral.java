package com.suhen.android.libble.peripheral;

/**
 * Created by liuqing
 * 2018/7/30.
 * Email: suhen0420@163.com
 */
public interface IPeripheral {
    void onCreate();

    boolean isSupportBle();

    void setup();

    void preparePair();

    boolean isConnected();

    void onDestroy();

    void sendBleBytes(byte[] bytes);
}
