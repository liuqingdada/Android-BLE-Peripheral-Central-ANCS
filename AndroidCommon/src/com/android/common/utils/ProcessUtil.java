package com.android.common.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.List;

public class ProcessUtil {
    private static String currentProcessName;

    /**
     * @return 当前进程名
     */
    @NonNull
    public static String getCurrentProcessName(@NonNull Context context) {
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName;
        }

        //1)通过Application的API获取当前进程名
        currentProcessName = getCurrentProcessNameByApplication();
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName;
        }

        //2)通过反射ActivityThread获取当前进程名
        currentProcessName = getCurrentProcessNameByActivityThread();
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName;
        }

        //3)通过ActivityManager获取当前进程名
        currentProcessName = getCurrentProcessNameByActivityManager(context);

        return currentProcessName;
    }


    /**
     * 通过Application新的API获取进程名，无需反射，无需IPC，效率最高。
     */
    public static String getCurrentProcessNameByApplication() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        }
        return null;
    }

    /**
     * 通过反射ActivityThread获取进程名，避免了ipc
     */
    @SuppressWarnings({"RedundantArrayCreation", "JavaReflectionInvocation"})
    @SuppressLint("PrivateApi")
    public static String getCurrentProcessNameByActivityThread() {
        String processName = null;
        try {
            final Method declaredMethod = Class.forName(
                    "android.app.ActivityThread",
                    false,
                    Application.class.getClassLoader()
            ).getDeclaredMethod("currentProcessName", (Class<?>[]) new Class[0]);
            declaredMethod.setAccessible(true);
            final Object invoke = declaredMethod.invoke(null, new Object[0]);
            if (invoke instanceof String) {
                processName = (String) invoke;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return processName;
    }

    /**
     * 通过ActivityManager 获取进程名，需要IPC通信
     */
    public static String getCurrentProcessNameByActivityManager(@NonNull Context context) {
        int pid = Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> runningAppList =
                    am.getRunningAppProcesses();
            if (runningAppList != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningAppList) {
                    if (processInfo.pid == pid) {
                        return processInfo.processName;
                    }
                }
            }
        }
        return "";
    }

    @NonNull
    public static String getCurrentProcessNameSuffix(@NonNull String processName) {
        String suffix = "";
        int index = processName.lastIndexOf(':');
        if (index > 0 && index + 1 < processName.length()) {
            suffix = processName.substring(index + 1);
        }
        return suffix;
    }

    public static boolean isMainProcess(@NonNull Context context) {
        return isMainProcess(getCurrentProcessName(context));
    }

    public static boolean isMainProcess(String processName) {
        return TextUtils.isEmpty(getCurrentProcessNameSuffix(processName));
    }
}
