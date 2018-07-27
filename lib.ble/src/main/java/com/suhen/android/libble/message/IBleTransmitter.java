package com.suhen.android.libble.message;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */
public interface IBleTransmitter<T extends BleMessage> {
    /**
     * @param bleMessage send to remote device
     */
    void sendBleMessage(T bleMessage);

    /**
     * @param bleMessage remote device send to me.
     * @return if this message is not null, send to remote device as response.
     */
    T onReceiveBleMessage(T bleMessage);

    void onConnected();

    void onDisconnected();
}
