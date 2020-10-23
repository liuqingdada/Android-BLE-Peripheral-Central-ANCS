package com.android.cooper.app.ble.remotecontrol.log;

import android.content.Context;
import android.os.Environment;

import com.android.common.utils.AppUtils;
import com.android.common.utils.ExecutorsKt;
import com.android.common.utils.FileLogger;
import com.android.common.utils.LogUtil;
import com.android.common.utils.ProcessUtil;

import java.io.File;
import java.util.Arrays;

/**
 * Created by cooper
 * 20-6-8.
 * Email: 1239604859@qq.com
 * 如果要用 Timber, 可以在这里实现 Timber.Tree
 */
public class LogTreeProxy {
    private static final String LOG_FILE_PREFIX = "rootinfo";
    private static final int MAX_LOG_FILE_COUNT = 30;
    public static final String LOG_DIR;

    static {
        LOG_DIR = initLogDir();
    }

    public LogTreeProxy(boolean debug, String processNameSuffix) {
        LogUtil.setDebug(debug);
        FileLogger fileLogger = new FileLogger(LOG_DIR, LOG_FILE_PREFIX, processNameSuffix);
        fileLogger.setFileCreateListener(this::clean);
        LogUtil.setFileLogger(fileLogger);
    }

    public void clean() {
        ExecutorsKt.serialExecute(() -> {
            if (LOG_DIR == null) {
                return;
            }
            File logDir = new File(LOG_DIR);
            File[] logFiles =
                    logDir.listFiles((dir, filename) -> filename.startsWith(LOG_FILE_PREFIX));

            if (logFiles != null && logFiles.length > MAX_LOG_FILE_COUNT) {
                Arrays.sort(
                        logFiles,
                        (lhs, rhs) -> Long.compare(rhs.lastModified(), lhs.lastModified())
                );
                int size = logFiles.length;
                for (int i = MAX_LOG_FILE_COUNT; i < size; i++) {
                    logFiles[i].delete();
                }
            }
        });
    }

    private static String initLogDir() {
        boolean mounted = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (mounted) {
            Context context = AppUtils.application.getApplicationContext();
            File logDir = new File(context.getExternalFilesDir(null), "logs");
            return logDir.getAbsolutePath();
        } else {
            return null;
        }
    }

    public static void main(String... args) {
        Context context = AppUtils.application.getApplicationContext();
        String processName = ProcessUtil.getCurrentProcessName(context);
        String processNameSuffix = ProcessUtil.getCurrentProcessNameSuffix(processName);
        boolean isMainpProcess = ProcessUtil.isMainProcess(context);
        if (isMainpProcess) {
            new LogTreeProxy(true, processNameSuffix);
        }
        CrashHandler.getInstance().init(context);
    }
}
