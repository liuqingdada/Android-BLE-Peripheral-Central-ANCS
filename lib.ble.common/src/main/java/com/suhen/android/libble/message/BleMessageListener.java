package com.suhen.android.libble.message;

/**
 * Created by cooper
 * 20-10-21.
 * Email: 1239604859@qq.com
 */
public interface BleMessageListener {
    void onReceiveMessage(int type, byte[] message);
}
