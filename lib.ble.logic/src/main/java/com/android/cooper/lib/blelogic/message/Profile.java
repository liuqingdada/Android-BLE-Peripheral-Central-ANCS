package com.android.cooper.lib.blelogic.message;

/**
 * Created by cooper
 * 20-10-21.
 * Email: 1239604859@qq.com
 */
public interface Profile {
    int KEY_EVENT_MESSAGE_TYPE = 1;

    void parse(byte[] payload);
}
