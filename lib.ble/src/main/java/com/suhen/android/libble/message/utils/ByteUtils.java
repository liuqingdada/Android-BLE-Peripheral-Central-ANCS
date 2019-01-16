package com.suhen.android.libble.message.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by liuqing
 * 2017/4/10.
 * Email: 1239604859@qq.com
 */

public class ByteUtils {
    /**
     * short到字节数组的转换.
     */
    public static byte[] shortToByte(short number) {
        int temp = number;
        byte[] b = new byte[2];
        for (int i = 0; i < b.length; i++) {
            b[i] = Integer.valueOf(temp & 0xff)
                          .byteValue();// 将最低位保存在最低位
            temp = temp >> 8;// 向右移8位
        }
        return b;
    }

    /**
     * 字节数组到short的转换.
     */
    public static short byteToShort(byte[] b) {
        short s;
        short s0 = (short) (b[0] & 0xff);// 最低位
        short s1 = (short) (b[1] & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        return s;
    }


    /**
     * int到字节数组的转换.
     */
    public static byte[] intToByte(int number) {
        int temp = number;
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[i] = Integer.valueOf(temp & 0xff)
                          .byteValue();// 将最低位保存在最低位
            temp = temp >> 8;// 向右移8位
        }
        return b;
    }

    /**
     * 字节数组到int的转换.
     */
    public static int byteToInt(byte[] b) {
        int s;
        int s0 = b[0] & 0xff;// 最低位
        int s1 = b[1] & 0xff;
        int s2 = b[2] & 0xff;
        int s3 = b[3] & 0xff;
        s3 <<= 24;
        s2 <<= 16;
        s1 <<= 8;
        s = s0 | s1 | s2 | s3;
        return s;
    }

    public static long bytes2long(byte[] b) {
        long temp;
        long res = 0;
        for (int i = 0; i < 8; i++) {
            res <<= 8;
            temp = b[i] & 0xff;
            res |= temp;
        }
        return res;
    }

    public static byte[] long2bytes(long num) {
        byte[] b = new byte[8];
        for (int i = 0; i < 8; i++) {
            b[i] = (byte) (num >>> (56 - (i * 8)));
        }
        return b;
    }


    //    /**
    //     * long类型转成byte数组
    //     */
    //    public static byte[] longToByte(long number) {
    //        byte[] b = new byte[8];
    //        for (int i = 0; i < b.length; i++) {
    //            b[i] = Long.valueOf(number & 0xff)
    //                       .byteValue();// 将最低位保存在最低位 temp = temp
    //            // >> 8;// 向右移8位
    //        }
    //        return b;
    //    }
    //
    //    /**
    //     * 字节数组到long的转换.
    //     */
    //    public static long byteToLong(byte[] b) {
    //        long s;
    //        long s0 = b[0] & 0xff;// 最低位
    //        long s1 = b[1] & 0xff;
    //        long s2 = b[2] & 0xff;
    //        long s3 = b[3] & 0xff;
    //        long s4 = b[4] & 0xff;// 最低位
    //        long s5 = b[5] & 0xff;
    //        long s6 = b[6] & 0xff;
    //        long s7 = b[7] & 0xff;
    //
    //        // s0不变
    //        s1 <<= 8;
    //        s2 <<= 16;
    //        s3 <<= 24;
    //        s4 <<= 8 * 4;
    //        s5 <<= 8 * 5;
    //        s6 <<= 8 * 6;
    //        s7 <<= 8 * 7;
    //        s = s0 | s1 | s2 | s3 | s4 | s5 | s6 | s7;
    //        return s;
    //    }

    /**
     * double到字节数组的转换.
     */
    public static byte[] doubleToByte(double num) {
        byte[] b = new byte[8];
        long l = Double.doubleToLongBits(num);
        for (int i = 0; i < 8; i++) {
            b[i] = Long.valueOf(l)
                       .byteValue();
            l = l >> 8;
        }
        return b;
    }

    /**
     * 字节数组到double的转换.
     */
    public static double getDouble(byte[] b) {
        long m;
        m = b[0];
        m &= 0xff;
        m |= ((long) b[1] << 8);
        m &= 0xffff;
        m |= ((long) b[2] << 16);
        m &= 0xffffff;
        m |= ((long) b[3] << 24);
        m &= 0xffffffffL;
        m |= ((long) b[4] << 32);
        m &= 0xffffffffffL;
        m |= ((long) b[5] << 40);
        m &= 0xffffffffffffL;
        m |= ((long) b[6] << 48);
        m &= 0xffffffffffffffL;
        m |= ((long) b[7] << 56);
        return Double.longBitsToDouble(m);
    }


    /**
     * float到字节数组的转换.
     */
    public static void floatToByte(float x) {
        //先用 Float.floatToIntBits(f)转换成int
    }

    /**
     * 字节数组到float的转换.
     */
    public static float getFloat(byte[] b) {
        // 4 bytes
        int accum = 0;
        for (int shiftBy = 0; shiftBy < 4; shiftBy++) {
            accum |= (b[shiftBy] & 0xff) << shiftBy * 8;
        }
        return Float.intBitsToFloat(accum);
    }

    /**
     * char到字节数组的转换.
     */
    public static byte[] charToByte(char c) {
        byte[] b = new byte[2];
        b[0] = (byte) ((c & 0xFF00) >> 8);
        b[1] = (byte) (c & 0xFF);
        return b;
    }

    /**
     * 字节数组到char的转换.
     */
    public static char byteToChar(byte[] b) {
        return (char) (((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
    }

    /**
     * string到字节数组的转换.
     */
    public static byte[] stringToByte(String str) throws UnsupportedEncodingException {
        return str.getBytes("UTF-8");
    }

    /**
     * 字节数组到String的转换.
     */
    public static String bytesToString(byte[] str) {
        String keyword = null;
        try {
            keyword = new String(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return keyword;
    }


    /**
     * object到字节数组的转换
     */
    public void testObject2ByteArray() throws IOException,
            ClassNotFoundException {
        // Object obj = "";
        Integer[] obj = { 1, 3, 4 };

        // // object to bytearray
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);
        oo.writeObject(obj);
        byte[] bytes = bo.toByteArray();
        bo.close();
        oo.close();
        System.out.println(Arrays.toString(bytes));

        Integer[] intArr = (Integer[]) testByteArray2Object(bytes);
        System.out.println(Arrays.asList(intArr));


        byte[] b2 = intToByte(123);
        System.out.println(Arrays.toString(b2));

        int a = byteToInt(b2);
        System.out.println(a);

    }

    /**
     * 字节数组到object的转换.
     */
    private Object testByteArray2Object(byte[] bytes) throws IOException,
            ClassNotFoundException {
        // byte[] bytes = null;
        Object obj;
        // bytearray to object
        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
        ObjectInputStream oi = new ObjectInputStream(bi);
        obj = oi.readObject();
        bi.close();
        oi.close();
        System.out.println(obj);
        return obj;
    }

    // ***********************************************************************
    // ***********************************************************************
    // ***********************************************************************

    public static <T> boolean linearIn(T[] outer, T[] inner) {
        return Arrays.asList(outer)
                     .containsAll(Arrays.asList(inner));
    }

    public static byte[] Byte2byte(Byte[] bytes) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = bytes[i];
        }
        return result;
    }

    public static Byte[] byte2Byte(byte[] bytes) {
        Byte[] result = new Byte[bytes.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = bytes[i];
        }
        return result;
    }

    public static boolean contains(byte[] outer, byte[] inner) {
        return linearIn(byte2Byte(outer), byte2Byte(inner));
    }

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
