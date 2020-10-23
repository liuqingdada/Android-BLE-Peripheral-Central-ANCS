package com.android.common.internalapi;

import android.content.Context;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.common.utils.LogUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PowerManagerIA {
    private static final String TAG = "PowerManagerIA";

    private static final int API_VERSION_1 = 1;
    private static final int API_VERSION_2 = 2;

    private static Method sMtd_asInterface;

    private static Class<?> sClass_IPowerManager;
    private static Method sMtd_reboot;
    private static int sVersion_reboot;
    private static Method sMtd_shutdown;
    private static int sVersion_shutdown;

    static {
        try {
            Class<?> stubClass = Class.forName("android.os.IPowerManager$Stub", false,
                    Thread.currentThread().getContextClassLoader());
            sMtd_asInterface = stubClass.getMethod("asInterface", IBinder.class);

            sClass_IPowerManager = Class.forName("android.os.IPowerManager", false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            LogUtil.w(TAG, "class not found", e);
        } catch (NoSuchMethodException e) {
            LogUtil.w(TAG, "method not found", e);
        }
    }

    private PowerManagerIA() {
        // nothing to do
    }

    /**
     * Get "android.os.IPowerManager" object from the service binder.
     * @return null will be returned if failed
     */
    @Nullable
    public static Object asInterface(@NonNull IBinder binder) {
        if (sMtd_asInterface != null) {
            try {
                return sMtd_asInterface.invoke(null, binder);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                LogUtil.w(TAG, "Failed to invoke #asInterface()", e);
            }
        } else {
            LogUtil.w(TAG, "#asInterface() not available");
        }
        return null;
    }

    /**
     * Get "android.os.IPowerManager" object from the service manager.
     * @return null will be returned if failed
     */
    @Nullable
    public static Object getIPowerManager() {
        IBinder binder = ServiceManagerIA.getService(Context.POWER_SERVICE);
        if (binder != null) {
            return asInterface(binder);
        }
        return null;
    }

    @VisibleForTesting
    static Method reflect_reboot() {
        if (sMtd_reboot != null || sClass_IPowerManager == null) {
            return sMtd_reboot;
        }

        try {
            try {
                // Android 2.2 ~ Android 4.1: void reboot(String reason);
                sMtd_reboot = sClass_IPowerManager.getMethod("reboot", String.class);
                sVersion_reboot = API_VERSION_1;
            } catch (NoSuchMethodException e) {
                // Android 4.2: void reboot(boolean confirm, String reason, boolean wait);
                sMtd_reboot = sClass_IPowerManager.getMethod("reboot",
                        boolean.class, String.class, boolean.class);
                sVersion_reboot = API_VERSION_2;
            }
        } catch (NoSuchMethodException e) {
            LogUtil.w(TAG, "method not found", e);
        }
        return sMtd_reboot;
    }

    /**
     * Reboot the device.
     * @param service The "android.os.IPowerManager" object.
     * @see #asInterface(IBinder)
     */
    public static void reboot(@NonNull Object service, @NonNull String reason) {
        reflect_reboot();
        if (sMtd_reboot != null) {
            try {
                if (sVersion_reboot == API_VERSION_1) {
                    sMtd_reboot.invoke(service, reason);
                } else if (sVersion_reboot == API_VERSION_2) {
                    sMtd_reboot.invoke(service, false, reason, false);
                } else {
                    LogUtil.e(TAG, "reboot, unknown api version: " + sVersion_reboot);
                }
            } catch (IllegalAccessException e) {
                LogUtil.w(TAG, "Failed to invoke #reboot()", e);
            } catch (InvocationTargetException e) {
                LogUtil.w(TAG, "Failed to invoke #reboot() more", e);
            }
        } else {
            LogUtil.w(TAG, "#reboot() not available");
        }
    }


    private static void reflect_shutdown() {
        if (sMtd_shutdown != null || sClass_IPowerManager == null ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return;
        }

        try {
            try {
                // Android 4.2: void shutdown(boolean confirm, boolean wait);
                sMtd_shutdown = sClass_IPowerManager.getMethod("shutdown",
                        boolean.class, boolean.class);
                sVersion_shutdown = API_VERSION_1;
            } catch (NoSuchMethodException e) {
                // Android 7.0: void shutdown(boolean confirm, String reason, boolean wait);
                sMtd_shutdown = sClass_IPowerManager.getMethod("shutdown",
                        boolean.class, String.class, boolean.class);
                sVersion_shutdown = API_VERSION_2;
            }
        } catch (NoSuchMethodException e) {
            LogUtil.w(TAG, "method not found", e);
        }
    }

    public static void shutdown(@NonNull Object service, String reason) {
        reflect_shutdown();
        if (sMtd_shutdown != null) {
            try {
                if (sVersion_shutdown == API_VERSION_1) {
                    sMtd_shutdown.invoke(service, false, false);
                } else if (sVersion_shutdown == API_VERSION_2) {
                    sMtd_shutdown.invoke(service, false, reason, false);
                } else {
                    LogUtil.e(TAG, "shutdown, unknown api version: " + sVersion_shutdown);
                }
            } catch (IllegalAccessException e) {
                LogUtil.w(TAG, "Failed to invoke #shutdown()", e);
            } catch (InvocationTargetException e) {
                LogUtil.w(TAG, "Failed to invoke #shutdown() more", e);
            }
        } else {
            LogUtil.w(TAG, "#shutdown() not available");
        }
    }
}