package com.android.common.utils;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

@SuppressWarnings({"WeakerAccess", "unused"})
public class DigestEncodingUtils {
    private static final char[] HEX_ARRAY_UPPERCASE = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] HEX_ARRAY_LOWERCASE = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Encode the data with HEX (Base16) encoding and with uppercase letters.
     */
    public static String encodeWithHex(byte[] bytes) {
        return encodeWithHex(bytes, true);
    }

    public static String encodeWithHex(byte[] bytes, boolean uppercase) {
        if (bytes == null) {
            return "null";
        }
        return encodeWithHex(bytes, 0, bytes.length, uppercase);
    }

    /**
     * Encode the data with HEX (Base16) encoding and with uppercase letters.
     */
    public static String encodeWithHex(@NonNull byte[] bytes, int startPos, int endPos) {
        return encodeWithHex(bytes, startPos, endPos, true);
    }

    public static String encodeWithHex(@NonNull byte[] bytes, int startPos, int endPos, boolean uppercase) {
        if (endPos > bytes.length) {
            endPos = bytes.length;
        }
        final int N = endPos - startPos;
        final char[] HEX_ARRAY = uppercase ? HEX_ARRAY_UPPERCASE : HEX_ARRAY_LOWERCASE;
        char[] hexChars = new char[N * 2];
        for (int i = startPos, j = 0; i < endPos; i++, j += 2) {
            int v = bytes[i] & 0xFF;
            hexChars[j] = HEX_ARRAY[v >>> 4];
            hexChars[j + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Encode the data with HEX (Base16) encoding and with uppercase letters.
     */
    public static String encodeWithHex(Byte[] bytes) {
        return encodeWithHex(bytes, true);
    }

    public static String encodeWithHex(Byte[] bytes, boolean uppercase) {
        if (bytes == null) {
            return "null";
        }
        return encodeWithHex(bytes, 0, bytes.length, uppercase);
    }

    /**
     * Encode the data with HEX (Base16) encoding and with uppercase letters.
     */
    public static String encodeWithHex(@NonNull Byte[] bytes, int startPos, int endPos) {
        return encodeWithHex(bytes, startPos, endPos, true);
    }

    public static String encodeWithHex(@NonNull Byte[] bytes, int startPos, int endPos, boolean uppercase) {
        if (endPos > bytes.length) {
            endPos = bytes.length;
        }
        final int N = endPos - startPos;
        final char[] HEX_ARRAY = uppercase ? HEX_ARRAY_UPPERCASE : HEX_ARRAY_LOWERCASE;
        char[] hexChars = new char[N * 2];
        for (int i = startPos, j = 0; i < endPos; i++, j += 2) {
            int v = bytes[i] & 0xFF;
            hexChars[j] = HEX_ARRAY[v >>> 4];
            hexChars[j + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] fromHexString(@NonNull String hexStr) {
        hexStr = hexStr.replace(" ", ""); // support spaces
        if (hexStr.length() % 2 != 0) {
            throw new IllegalArgumentException("Bad length: " + hexStr);
        }

        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = fromHexChar(hexStr, i * 2) << 4;
            int low = fromHexChar(hexStr, i * 2 + 1);
            result[i] = (byte) ((high | low) & 0xFF);
        }
        return result;
    }

    private static int fromHexChar(String hexStr, int index) {
        char ch = hexStr.charAt(index);
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        } else if (ch >= 'a' && ch <= 'f') {
            return 10 + (ch - 'a');
        } else if (ch >= 'A' && ch <= 'F') {
            return 10 + (ch - 'A');
        } else {
            throw new IllegalArgumentException("Not hex string: " + hexStr);
        }
    }

    public static String sha1(String text)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return hash(text, "SHA-1");
    }

    public static String sha1(byte[] data)
            throws NoSuchAlgorithmException {
        return hash(data, "SHA-1");
    }

    public static String md5(final String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return hash(text, "MD5");
    }

    public static String hash(String text, String algorithm)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return hash(text.getBytes("UTF-8"), algorithm);
    }

    public static String hash(byte[] data, String algorithm)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(data);
        byte messageDigest[] = digest.digest();
        return encodeWithHex(messageDigest, false);
    }

    /**
     * The caller should care about closing the stream.
     */
    public static String md5(final InputStream stream)
            throws NoSuchAlgorithmException, IOException {
        if (stream == null) {
            throw new IllegalArgumentException("Invalid input stream!");
        }
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = stream.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        byte[] digest = complete.digest();
        return encodeWithHex(digest, false);
    }

    public static String md5(final File file) throws NoSuchAlgorithmException, IOException {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            return md5(stream);
        } finally {
            IoUtils.closeQuietly(stream);
        }
    }

    public static long computeCrc32(@NonNull byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }
}
