package com.suhen.android.libble.message.utils;

import com.jcraft.jzlib.JZlib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by suhen
 * 17-6-27.
 * Email: 1239604859@qq.com
 */

public class ByteZipUtil {
    /***
     * 压缩GZip
     */
    public static byte[] gZip(byte[] data) {
        /*
         * Java.util.zip.DataFormatException: stream error
         * 在4.4.4手机回报错，其他手机没问题
         * 解决办法 gzip.finish(); gzip.flush();去掉其中一个
         */
        byte[] b = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(data);
            gzip.finish();
            //gzip.flush();
            gzip.close();
            b = bos.toByteArray();
            bos.flush();
            bos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return b;
    }

    /***
     * 解压GZip
     */
    public static byte[] unGZip(byte[] data) {
        byte[] b = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            GZIPInputStream gzip = new GZIPInputStream(bis);
            byte[] buf = new byte[1024];
            int num;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((num = gzip.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, num);
            }
            b = baos.toByteArray();
            baos.flush();
            baos.close();
            gzip.close();
            bis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return b;
    }

    /***
     * 压缩Zip
     */
    public static byte[] zip(byte[] data) {
        byte[] b = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(bos);
            ZipEntry entry = new ZipEntry("zip");
            entry.setSize(data.length);
            zip.putNextEntry(entry);
            zip.write(data);
            zip.closeEntry();
            zip.close();
            b = bos.toByteArray();
            bos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return b;
    }

    /***
     * 解压Zip
     */
    public static byte[] unZip(byte[] data) {
        byte[] b = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ZipInputStream zip = new ZipInputStream(bis);
            while (zip.getNextEntry() != null) {
                byte[] buf = new byte[1024];
                int num;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while ((num = zip.read(buf, 0, buf.length)) != -1) {
                    baos.write(buf, 0, num);
                }
                b = baos.toByteArray();
                baos.flush();
                baos.close();
            }
            zip.close();
            bis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return b;
    }

//    /**
//     * 压缩BZip2
//     */
//
//    /**
//     * 解压BZip2
//     */

    /**
     * 把字节数组转换成16进制字符串
     */
    public static String bytesToHexString(byte[] bArray) {
        StringBuilder sb = new StringBuilder(bArray.length);
        String sTemp;
        for (byte aBArray : bArray) {
            sTemp = Integer.toHexString(0xFF & aBArray);
            if (sTemp.length() < 2) { sb.append(0); }
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * jzlib
     * 压缩数据
     */
    public static byte[] jzlib(byte[] object) {
        byte[] data = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DeflaterOutputStream zOut = new DeflaterOutputStream(out, new Deflater(
                    JZlib.Z_DEFAULT_COMPRESSION));
            //ZOutputStream zOut = new ZOutputStream(out,
            //                                       JZlib.Z_DEFAULT_COMPRESSION);
            DataOutputStream objOut = new DataOutputStream(zOut);
            objOut.write(object);
            objOut.flush();
            zOut.close();
            data = out.toByteArray();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * jzlib
     * 解压被压缩的数据
     */
    public static byte[] unjzlib(byte[] object) {
        byte[] data = null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(object);
            InflaterInputStream zIn = new InflaterInputStream(in);
            //ZInputStream zIn = new ZInputStream(in);
            byte[] buf = new byte[1024];
            int num;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((num = zIn.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, num);
            }
            data = baos.toByteArray();
            baos.flush();
            baos.close();
            zIn.close();
            in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
