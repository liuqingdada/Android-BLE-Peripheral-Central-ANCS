package com.suhen.android.libble.central.callback;

/**
 * Created by yangliuqing
 * 2019/1/13.
 * Email: 1239604859@qq.com
 */
public interface BleMtuChangedCallback {
    void onSetMTUFailure(int reason);

    void onMtuChanged(int mtu, int status);
}
