package com.suhen.android.libble.utils;

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

    public T getOwner() {
        return mOwner.get();
    }
}