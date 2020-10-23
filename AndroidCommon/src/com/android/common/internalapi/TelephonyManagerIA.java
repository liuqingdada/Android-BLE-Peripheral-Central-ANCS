package com.android.common.internalapi;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.android.common.utils.LogUtil;
import com.android.common.utils.ReflectionUtils;

import java.lang.reflect.Method;

import static android.content.Context.TELEPHONY_SERVICE;

public class TelephonyManagerIA {
    private static final String TAG = "TelephonyManagerIA";

    public static void toggleRadioOnOff(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
            Method toggleRadioOnOff = ReflectionUtils.findMethod(TelephonyManager.class, "toggleRadioOnOff");
            toggleRadioOnOff.invoke(telephonyManager);
        } catch (Exception e) {
            LogUtil.w(TAG, "failed to get method #isRadioOn", e);
        }
    }

    public static boolean isRadioOn(Context context) {
        boolean result = false;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
            Method isRadioOnMethod = ReflectionUtils.findMethod(TelephonyManager.class, "isRadioOn");
            result = (boolean) isRadioOnMethod.invoke(telephonyManager);
        } catch (Exception e) {
            LogUtil.w(TAG, "failed to get method #isRadioOn", e);
        }
        return result;
    }

    public static void setSimPowerState(Context context, boolean state) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= 28) {
                // For Android P
                Method setSimPowerStateMethod = ReflectionUtils.findMethod(TelephonyManager.class, "setSimPowerState", int.class);
                setSimPowerStateMethod.invoke(telephonyManager, state ? 1 : 0);
            } else {
                Method setSimPowerStateMethod = ReflectionUtils.findMethod(TelephonyManager.class, "setSimPowerState", boolean.class);
                setSimPowerStateMethod.invoke(telephonyManager, state);
            }
        } catch (Exception e) {
            LogUtil.w(TAG, "failed to get method #setSimPowerState", e);
        }
    }
}


