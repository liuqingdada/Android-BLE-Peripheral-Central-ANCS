package com.suhen.android.libble.message;

import com.android.common.utils.LogUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by cooper
 * 20-10-21.
 * Email: 1239604859@qq.com
 */
public class BleMessageDecoder {
    private static final String TAG = "BleMessageDecoder";
    protected ByteBuffer cache = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

    private final BleMessageListener bleMessageListener;

    public BleMessageDecoder(BleMessageListener listener) {
        bleMessageListener = listener;
    }

    public void stash(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        byte[] payload = Arrays.copyOfRange(data, 1, data.length);

        switch (data[0]) {
            case BleMessage.START_DEFAULT:
                cache.clear();

                cache.put(payload);
                break;
            case BleMessage.START_ONE_PACKAGE_MSG:
                cache.clear();
                cache.put(payload);
                onDecodeMessage(cache.array(), cache.position());
                break;

            case BleMessage.END_DEFAULT:
                cache.put(payload);
                onDecodeMessage(cache.array(), cache.position());
                break;
        }

        if (cache.capacity() - cache.position() <= data.length * 3) {
            ByteBuffer newCache = ByteBuffer.allocate(cache.capacity() * 2)
                    .order(ByteOrder.LITTLE_ENDIAN);
            newCache.put(cache);
            cache = newCache;
        }
    }

    private void onDecodeMessage(byte[] message, int position) {
        if (position < 4) {
            LogUtil.w(TAG, "Illegal ble message.");
            return;
        }

        byte[] typeData = Arrays.copyOfRange(message, 0, 4);
        byte[] payload = Arrays.copyOfRange(message, 4, position);

        ByteBuffer buffer = ByteBuffer.wrap(typeData).order(ByteOrder.LITTLE_ENDIAN);
        int type = buffer.getInt();

        bleMessageListener.onReceiveMessage(type, payload);
    }
}
