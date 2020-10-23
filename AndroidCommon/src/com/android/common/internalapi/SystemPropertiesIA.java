package com.android.common.internalapi;

import androidx.annotation.VisibleForTesting;

import com.android.common.utils.LogUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SystemPropertiesIA {
    private static final String TAG = "SystemPropertiesIA";

    private static Class<?> sClass_SystemProperties;

    private static Method sMtd_get;
    private static Method sMtd_getInt;
    private static Method sMtd_getLong;
    private static Method sMtd_getBoolean;

    private static Method sMtd_set;

    static {
        try {
            sClass_SystemProperties = Class.forName("android.os.SystemProperties", false,
                    Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            LogUtil.w(TAG, "Failed to reflect SystemProperties", e); // unexpected
        }
    }

    private SystemPropertiesIA() {
        // nothing to do
    }

    @VisibleForTesting
    static Method reflect_get() {
        if (sMtd_get != null || sClass_SystemProperties == null) {
            return sMtd_get;
        }

        try {
            sMtd_get = sClass_SystemProperties.getMethod("get", String.class, String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); // unexpected
        }
        return sMtd_get;
    }

    public static String get(String key, String def) {
        reflect_get();

        if (sMtd_get != null) {
            try {
                Object result = sMtd_get.invoke(null, key, def);
                return (String) result;
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace(); // unexpected
            }
        } else {
            // unexpected
            LogUtil.w(TAG, "#get(String, String) not found");
        }
        return def;
    }

    @VisibleForTesting
    static Method reflect_getInt() {
        if (sMtd_getInt != null || sClass_SystemProperties == null) {
            return sMtd_getInt;
        }

        try {
            sMtd_getInt = sClass_SystemProperties.getMethod("getInt", String.class, int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); // unexpected
        }
        return sMtd_getInt;
    }

    public static int getInt(String key, int def) {
        reflect_getInt();

        if (sMtd_getInt != null) {
            try {
                Object result = sMtd_getInt.invoke(null, key, def);
                return (Integer) result;
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace(); // unexpected
            }
        } else {
            // unexpected
            LogUtil.w(TAG, "#getInt(String, int) not found");
        }
        return def;
    }

    @VisibleForTesting
    static Method reflect_getLong() {
        if (sMtd_getLong != null || sClass_SystemProperties == null) {
            return sMtd_getLong;
        }

        try {
            sMtd_getLong = sClass_SystemProperties.getMethod("getLong", String.class, long.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); // unexpected
        }
        return sMtd_getLong;
    }

    public static long getLong(String key, long def) {
        reflect_getLong();

        if (sMtd_getLong != null) {
            try {
                Object result = sMtd_getLong.invoke(null, key, def);
                return (Long) result;
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace(); // unexpected
            }
        } else {
            // unexpected
            LogUtil.w(TAG, "#getLong(String, long) not found");
        }
        return def;
    }

    @VisibleForTesting
    static Method reflect_getBoolean() {
        if (sMtd_getBoolean != null || sClass_SystemProperties == null) {
            return sMtd_getBoolean;
        }

        try {
            sMtd_getBoolean = sClass_SystemProperties.getMethod("getBoolean", String.class, boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); // unexpected
        }
        return sMtd_getBoolean;
    }

    public static boolean getBoolean(String key, boolean def) {
        reflect_getBoolean();

        if (sMtd_getBoolean != null) {
            try {
                Object result = sMtd_getBoolean.invoke(null, key, def);
                return (Boolean) result;
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace(); // unexpected
            }
        } else {
            LogUtil.w(TAG, "#getBoolean(String, boolean) not found");
        }
        return def;
    }

    @VisibleForTesting
    static Method reflect_set() {
        if (sMtd_set != null || sClass_SystemProperties == null) {
            return sMtd_set;
        }

        try {
            sMtd_set = sClass_SystemProperties.getMethod("set", String.class, String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); // unexpected
        }
        return sMtd_set;
    }

    public static void set(String key, String def) {
        reflect_set();

        if (sMtd_set != null) {
            try {
                sMtd_set.invoke(null, key, def);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace(); // unexpected
            }
        } else {
            // unexpected
            LogUtil.w(TAG, "#set(String, String) not found");
        }
    }
}
