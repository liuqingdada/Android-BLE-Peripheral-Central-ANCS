package com.android.common.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsUtils {
    public static void remove(Context cxt, String prefsName, String key) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }

    public static String getString(Context cxt, String prefsName, String key, String defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getString(key, defValue);
    }

    public static int getInt(Context cxt, String prefsName, String key, int defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getInt(key, defValue);
    }

    public static long getLong(Context cxt, String prefsName, String key, long defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getLong(key, defValue);
    }

    public static boolean getBoolean(Context cxt, String prefsName, String key, boolean defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, defValue);
    }

    public static float getFloat(Context cxt, String prefsName, String key, float defValue) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getFloat(key, defValue);
    }

    public static void putString(Context cxt, String prefsName, String key, String value) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    public static void putInt(Context cxt, String prefsName, String key, int value) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putInt(key, value).apply();
    }

    public static void putLong(Context cxt, String prefsName, String key, long value) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putLong(key, value).apply();
    }

    public static void putBoolean(Context cxt, String prefsName, String key, boolean value) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
    }

    public static void putFloat(Context cxt, String prefsName, String key, float value) {
        SharedPreferences prefs = cxt.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putFloat(key, value).apply();
    }
}
