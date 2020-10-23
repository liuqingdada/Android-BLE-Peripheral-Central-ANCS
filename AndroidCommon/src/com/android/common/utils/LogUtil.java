package com.android.common.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.common.internalapi.SystemPropertiesIA;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import timber.log.Timber;

/**
 * LogUtil is designed to work in JVM. So, no Android specific things allowed.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class LogUtil {
    private static final String GLOBAL_LOG_TAG = "MobvoiGlobalDebug";

    private static boolean sJvmLogger = false;

    public static void enableJvmLogger() {
        sJvmLogger = true;
    }

    public static boolean isJvmLogger() {
        return sJvmLogger;
    }

    public static void setFileLogger(FileLogger fileLogger) {
        if (!sJvmLogger) {
            AndroidLogger.setFileLogger(fileLogger);
        }
    }

    public static void setDebug(boolean debug) {
        if (!sJvmLogger) {
            AndroidLogger.setDebug(debug);
        }
    }

    public static boolean isDebug() {
        return sJvmLogger || AndroidLogger.isDebug();
    }

    public static String getLogDir() {
        return AndroidLogger.getLogDir();
    }

    /**
     * @param level Where level is either VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT,
     *              or SUPPRESS. SUPPRESS will turn off all logging for your tag.
     */
    public static void setGlobalLogTag(String level) {
        SystemPropertiesIA.set("log.tag." + GLOBAL_LOG_TAG, level);
    }

    public static void setUserLogLevel(int level) {
        String levelStr = "SUPPRESS";
        switch (level) {
            case Log.VERBOSE:
                levelStr = "VERBOSE";
                break;
            case Log.DEBUG:
                levelStr = "DEBUG";
                break;
            case Log.INFO:
                levelStr = "INFO";
                break;
            case Log.WARN:
                levelStr = "WARN";
                break;
            case Log.ERROR:
                levelStr = "ERROR";
                break;
            case Log.ASSERT:
                levelStr = "ASSERT";
                break;
        }
        setGlobalLogTag(levelStr);
    }

    public static void v(String tag, String msg) {
        log(Log.VERBOSE, tag, msg, null);
    }

    public static void v(String tag, String msg, Throwable tr) {
        log(Log.VERBOSE, tag, msg, tr);
    }

    /**
     * Record logs using String#format()
     */
    public static void v(String tag, String msgFormat, Object... args) {
        log(Log.VERBOSE, tag, msgFormat, null, args);
    }

    public static void d(String tag, String msg) {
        log(Log.DEBUG, tag, msg, null);
    }

    public static void d(String tag, String msg, Throwable tr) {
        log(Log.DEBUG, tag, msg, tr);
    }

    /**
     * Record logs using String#format()
     */
    public static void d(String tag, String msgFormat, Object... args) {
        log(Log.DEBUG, tag, msgFormat, null, args);
    }

    public static void i(String tag, String msg) {
        log(Log.INFO, tag, msg, null);
    }

    /**
     * Record logs using String#format()
     */
    public static void i(String tag, String msgFormat, Object... args) {
        log(Log.INFO, tag, msgFormat, null, args);
    }

    public static void w(String tag, String msg) {
        log(Log.WARN, tag, msg, null);
    }

    public static void w(String tag, String msg, Throwable tr, Object... args) {
        log(Log.WARN, tag, msg, tr, args);
    }

    /**
     * Record logs using String#format()
     */
    public static void w(String tag, String msgFormat, Object... args) {
        log(Log.WARN, tag, msgFormat, null, args);
    }

    public static void e(String tag, String msg) {
        log(Log.ERROR, tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable tr) {
        log(Log.ERROR, tag, msg, tr);
    }

    public static void e(String tag, String msg, Throwable tr, Object... args) {
        log(Log.ERROR, tag, msg, tr, args);
    }

    /**
     * Record logs using String#format()
     */
    public static void e(String tag, String msgFormat, Object... args) {
        log(Log.ERROR, tag, msgFormat, null, args);
    }

    public static void wtf(String tag, String msg) {
        log(Log.ASSERT, tag, msg, null);
    }

    public static void log(int level, String tag, String msg, Throwable tr, Object... args) {
        if (sJvmLogger) {
            if (args != null && args.length > 0) {
                msg = String.format(Locale.US, msg, args);
            }
            System.out.println("[" + tag + "] " + msg);
            if (tr != null) {
                tr.printStackTrace();
            }
        } else {
            AndroidLogger.log(level, tag, msg, tr, args);
        }
    }

    /**
     * Don't use Log.getStackTraceString(), which will hide java.net.UnknownHostException.
     */
    public static String getStackTraceString(Throwable tr) {
        StringWriter sw = new StringWriter(4096);
        PrintWriter pw = new PrintWriter(sw, false);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private static class AndroidLogger {
        private static boolean sDebug = !Build.TYPE.equals("user");
        private static FileLogger sFileLogger;

        public static void setFileLogger(FileLogger fileLogger) {
            sFileLogger = fileLogger;
        }

        public static void setDebug(boolean debug) {
            sDebug = debug;
            if (!debug && sFileLogger != null) {
                sFileLogger.close();
            }
        }

        public static boolean isDebug() {
            return sDebug;
        }

        public static String getLogDir() {
            return sFileLogger == null ? null : sFileLogger.getLogDir();
        }

        private static void logToFile(String tag, String msg, Throwable tr) {
            if (sDebug && sFileLogger != null) {
                sFileLogger.logToFile(tag, msg, tr);
            }
        }

        public static void log(int level, String tag, String msg, Throwable tr, Object... args) {
            if (showLog(level, tag)) {
                if (args != null && args.length > 0) {
                    msg = String.format(Locale.US, msg, args);
                }
                logInternal(level, tag, msg, tr);
            }
        }

        private static boolean showLog(int level, String tag) {
            return isLoggable(tag, level) || sDebug;
        }

        private static void logInternal(int level, String tag, String msg, Throwable tr) {
            if (tr == null) {
                Log.println(level, tag, msg);
            } else {
                Log.println(level, tag, msg + '\n' + getStackTraceString(tr));
            }
            logToFile(tag, msg, tr);
        }

        @SuppressLint("LogNotTimber")
        private static boolean isLoggable(String tag, int level) {
            boolean isLoggable = false;
            try {
                isLoggable = Log.isLoggable(tag, level) || Log.isLoggable(GLOBAL_LOG_TAG, level);
            } catch (Exception e) {
                if (sDebug) {
                    throw e;
                } else {
                    Log.e(tag, "Can't detect is loggable.", e);
                }
            }
            return isLoggable;
        }
    }

    public static class TimberTree extends Timber.Tree {
        @Override
        protected void log(int priority, @Nullable String tag, @NonNull String message,
                           @Nullable Throwable t) {
            LogUtil.log(priority, tag, message, t);
        }
    }
}
