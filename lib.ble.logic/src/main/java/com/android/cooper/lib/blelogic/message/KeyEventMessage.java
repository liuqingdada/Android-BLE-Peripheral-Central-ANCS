package com.android.cooper.lib.blelogic.message;

import com.android.common.utils.LogUtil;
import com.android.cooper.lib.blelogic.utils.RcUtils;
import com.suhen.android.libble.message.BleMessage;

import java.nio.ByteBuffer;

/**
 * @author: cooper
 * Date: 20-10-21.
 * Email: yangliuqing@xiaomi.com
 */
public class KeyEventMessage extends BleMessage implements Profile {
    private static final String TAG = "KeyEventMessage";
    public static final int TYPE = KEY_EVENT_MESSAGE_TYPE;

    /**
     * {@link android.view.KeyEvent}
     */
    private int keyCode;

    private KeyEventMessage() {
    }

    public KeyEventMessage(int keyCode) {
        this.keyCode = keyCode;
    }

    @Override
    protected byte[] payload() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(TYPE)
                .putInt(keyCode);
        return buffer.array();
    }

    @Override
    public void parse(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int keyCode = buffer.getInt();
        LogUtil.d(TAG, "parse: " + keyCode);
        try {
            RcUtils.dispatchKeyEvent(keyCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static KeyEventMessage build() {
        return new KeyEventMessage();
    }
}
