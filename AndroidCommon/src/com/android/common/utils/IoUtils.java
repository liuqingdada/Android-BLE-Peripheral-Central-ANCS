package com.android.common.utils;

import android.content.res.AssetFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipFile;

public class IoUtils {
    private static final int IO_BUF_SIZE = 1024 * 16; // 16KB

    private IoUtils() {
    }

    /**
     * Close the closeable target and eat possible exceptions.
     *
     * @param target The target to close. Can be null.
     */
    public static void closeQuietly(@Nullable Closeable target) {
        try {
            if (target != null) {
                target.close();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Before Android 4.4, ZipFile doesn't implement the interface "java.io.Closeable".
     *
     * @param target The target to close. Can be null.
     */
    public static void closeQuietly(@Nullable ZipFile target) {
        try {
            if (target != null) target.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static void closeQuietly(@Nullable AssetFileDescriptor fd) {
        try {
            if (fd != null)
                fd.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static byte[] readAllBytes(@NonNull InputStream is) throws IOException {
        ByteArrayOutputStream bytesBuf = new ByteArrayOutputStream(1024);
        int bytesReaded;
        byte[] buf = new byte[1024 * 8];
        while ((bytesReaded = is.read(buf, 0, buf.length)) != -1) {
            bytesBuf.write(buf, 0, bytesReaded);
        }
        return bytesBuf.toByteArray();
    }

    /**
     * Copy data from the input stream to the output stream.</br>
     * Note: This method will not close the input stream and output stream.
     */
    public static void copyStream(@NonNull InputStream is, @NonNull OutputStream os)
            throws IOException {
        byte[] buffer = new byte[IO_BUF_SIZE];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        os.flush();
    }

    public static void saveStream(@NonNull InputStream is, @NonNull String filepath)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(filepath);
        try {
            copyStream(is, fos);
        } finally {
            IoUtils.closeQuietly(fos);
        }
    }

    public static void copyFile(@NonNull String sourceFilepath, @NonNull String destFilepath)
            throws IOException {
        FileInputStream fis = new FileInputStream(sourceFilepath);
        try {
            saveStream(fis, destFilepath);
        } finally {
            closeQuietly(fis);
        }
    }

    public static void saveAsFile(@NonNull String content, @NonNull String filePath)
            throws IOException {
        FileWriter fw = new FileWriter(filePath);
        try {
            fw.write(content);
            fw.flush();
        } finally {
            closeQuietly(fw);
        }
    }
}
