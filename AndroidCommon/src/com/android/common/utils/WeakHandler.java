package com.android.common.utils;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

public abstract class WeakHandler<T> extends Handler {
    private WeakReference<T> mOwner;

    public WeakHandler(T owner) {
        mOwner = new WeakReference<>(owner);
    }

    public WeakHandler(T owner, Looper looper) {
        super(looper);
        mOwner = new WeakReference<>(owner);
    }

    protected T getOwner() {
        return mOwner.get();
    }
}