package com.android.cooper.app.miottvdemo.log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Process;

import com.android.common.utils.LogUtil;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CrashHandler implements UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";

    private UncaughtExceptionHandler mDefaultHandler;

    @SuppressLint("StaticFieldLeak")
    private static CrashHandler sInstance;

    private Context mContext;

    private Map<String, String> mDeviceInfo = new HashMap<>();

    private DateFormat mFormatter = new SimpleDateFormat("yyMMdd", Locale.getDefault());
    private DateFormat mCurrentTimeFormatter
            = new SimpleDateFormat("yy-MM-dd HH:mm:ss:SSS", Locale.getDefault());

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        if (sInstance == null) {
            sInstance = new CrashHandler();
        }
        return sInstance;
    }

    public void init(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NotNull Thread thread, @NotNull Throwable tx) {
        handleException(tx);
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, tx);
        }
    }

    public boolean handleException(Throwable tx) {
        if (tx == null) {
            return false;
        }

        LogUtil.d(TAG, "crash happens", tx);
        collectDeviceInfo(mContext);
        saveCrashInfoToFile(tx);
        return true;
    }

    private void collectDeviceInfo(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(
                    mContext.getPackageName(),
                    PackageManager.GET_ACTIVITIES
            );
            if (info != null) {
                String versionName = info.versionName == null ? "null" : info.versionName;
                String versionCode = info.versionCode + "";
                mDeviceInfo.put("versionName", versionName);
                mDeviceInfo.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            // ignore
        }

        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                mDeviceInfo.put(field.getName(), field.get(null).toString());
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private String saveCrashInfoToFile(Throwable tx) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : mDeviceInfo.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
            sb.append("\n");
        }
        sb.append(mCurrentTimeFormatter.format(System.currentTimeMillis()));
        sb.append("\npid=").append(Process.myPid());
        sb.append("\n");

        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        tx.printStackTrace(pw);
        Throwable cause = tx.getCause();
        while (cause != null) {
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }
        pw.close();
        String result = writer.toString();
        sb.append(result);
        sb.append("\n\n");

        try {
            String time = mFormatter.format(new Date());
            String fileName = "crash_log_" + time + ".log";
            if (LogTreeProxy.LOG_DIR != null) {
                String path = LogTreeProxy.LOG_DIR;
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(new File(path, fileName), true);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception ex) {
            // ignore
        }

        return null;
    }
}
