package com.suhen.android.libble.message.utils;

/**
 * Created by liuqing
 * 2017/4/10.
 * Email: 1239604859@qq.com
 */

public class ByteUtil {

    public static byte[] combine2Bytes(byte[] bytes1, byte[] bytes2) {
        byte[] bytes3 = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, bytes3, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bytes3, bytes1.length, bytes2.length);
        return bytes3;
    }

    public static byte[] combineBytes(byte[]... bs) {
        int length = 0;
        for (byte[] bytes : bs) {
            length += bytes.length;
        }

        byte[] bys = new byte[length];
        int curr = 0;

        for (byte[] bytes : bs) {
            System.arraycopy(bytes, 0, bys, curr, bytes.length);
            curr += bytes.length;
        }
        return bys;
    }

    // 完整的判断中文汉字和符号
    public static boolean isChinese(String strName) {
        char[] ch = strName.toCharArray();
        for (char c : ch) {
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }

    // 根据Unicode编码完美的判断中文汉字和符号
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock
                .CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub ==
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character
                .UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    /**
     * Finds a sub array in a large array
     *
     * @return index of sub array
     */
    public static <T> int findArray(T[] largeArray, T[] subArray) {

        /* If any of the arrays is empty then not found */
        if (largeArray.length == 0 || subArray.length == 0) {
            return -1;
        }

        /* If subarray is larger than large array then not found */
        if (subArray.length > largeArray.length) {
            return -1;
        }

        for (int i = 0; i < largeArray.length; i++) {
            /* Check if the next element of large array is the same as the first element of
            subarray */
            if (largeArray[i] == subArray[0]) {

                boolean subArrayFound = true;
                for (int j = 0; j < subArray.length; j++) {
                    /* If outside of large array or elements not equal then leave the loop */
                    if (largeArray.length <= i + j || subArray[j] != largeArray[i + j]) {
                        subArrayFound = false;
                        break;
                    }
                }

                /* Sub array found - return its index */
                if (subArrayFound) {
                    return i;
                }

            }
        }

        /* Return default value */
        return -1;
    }

    /**
     * can replace the Native data types
     */
    public static int findArray(byte[] largeArray, byte[] subArray) {

        /* If any of the arrays is empty then not found */
        if (largeArray.length == 0 || subArray.length == 0) {
            return -1;
        }

        /* If subarray is larger than large array then not found */
        if (subArray.length > largeArray.length) {
            return -1;
        }

        for (int i = 0; i < largeArray.length; i++) {
            /* Check if the next element of large array is the same as the first element of
            subarray */
            if (largeArray[i] == subArray[0]) {

                boolean subArrayFound = true;
                for (int j = 0; j < subArray.length; j++) {
                    /* If outside of large array or elements not equal then leave the loop */
                    if (largeArray.length <= i + j || subArray[j] != largeArray[i + j]) {
                        subArrayFound = false;
                        break;
                    }
                }

                /* Sub array found - return its index */
                if (subArrayFound) {
                    return i;
                }

            }
        }

        /* Return default value */
        return -1;
    }

    public static boolean isChar(char c) {
        String s = String.valueOf(c);
        boolean b1 = isChinese(c);
        boolean b2 = !FilterStr.filterAlphabet(s)
                               .equals("");
        boolean b3 = !FilterStr.filterNumber(s)
                               .equals("");
        boolean b4 = FilterStr.filterSymbol(s);
        return b1 || b2 || b3 || b4;
    }
}
