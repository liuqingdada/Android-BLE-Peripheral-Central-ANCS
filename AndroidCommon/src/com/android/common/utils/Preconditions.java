// Copyright 2014 Mobvoi Inc. All Rights Reserved.
package com.android.common.utils;

import android.os.Looper;

/**
 * Precondition
 *
 * @author Chaofei Fan, <cfan@mobvoi.com>
 * @date 2015/01/23
 */
public class Preconditions {
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    public static void checkState(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

    public static void checkMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("MUST be invoked in UI thread");
        }
    }

    public static void checkNonMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("MUST be invoked in non-UI thread");
        }
    }
}
