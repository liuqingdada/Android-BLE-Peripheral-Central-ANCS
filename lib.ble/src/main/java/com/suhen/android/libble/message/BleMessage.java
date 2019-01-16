package com.suhen.android.libble.message;

import com.suhen.android.libble.message.utils.ByteUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */
public abstract class BleMessage {
    ////////////////////////////////////////////////////////////////
    public static final int SIZE = 20;
    public static final byte[] PACKET_DEFAULT = { (byte) 0x00 };
    public static final byte[] PACKET_END = { (byte) 0x01 };

    public static final byte DEFAULT = (byte) 0x00;

    public static final byte START_DEFAULT = (byte) 0xff;
    public static final byte START_DEFAULT_GZIP = (byte) 0xfe;
    public static final byte START_BYTE_MSG = (byte) 0xfd;
    public static final byte START_BYTE_MSG_GZIP = (byte) 0xfc;
    public static final byte START_ONE_PACKAGE_MSG = (byte) 0xfb;

    public static final byte END_DEFAUL = (byte) 0x01;
    ////////////////////////////////////////////////////////////////

    private byte[] mData;

    protected byte[] byteToBitArray(byte b) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }

    /**
     * @return 不 会 忽 略 前 面 的 0
     */
    protected String byteToBitString(byte b) {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 7; i >= 0; i--) {
            sb.append((byte) ((b >> (i)) & 0x1));
        }
        return sb.toString();
    }

    /**
     * @return 忽 略 前 面 的 0
     */
    protected String byte2String(byte b) {
        return Integer.toBinaryString(b);
    }

    /**
     * 每一个包的大小按照实际情况进行修改
     *
     * @param data       要分包的数据
     * @param start_type 开始类型
     */
    public List<byte[]> subpackage(byte[] data, byte start_type) {
        List<byte[]> packets = new ArrayList<>();
        int size = SIZE - 1; // 每一个包的大小 = size + 1

        if (data == null || data.length == 0) { // 确保能分包
            return packets;
        }

        if (data.length < size) {
            data = ByteUtils.combine2Bytes(data, new byte[size - data.length]);
        }

        int length = data.length;
        int remain = length % size;
        int time = length / size;

        /*
         * 分包并添加包头
         */

        for (int i = 0; i < time; i++) {
            byte[] bytes = Arrays.copyOfRange(data, i * size, (i + 1) * size);
            bytes = ByteUtils.combine2Bytes(PACKET_DEFAULT, bytes);
            packets.add(bytes);
        }
        if (remain > 0) {
            byte[] bytes = Arrays.copyOfRange(data, length - remain, length);
            bytes = ByteUtils.combine2Bytes(PACKET_DEFAULT, bytes);
            packets.add(bytes);
        }

        byte[] start = packets.get(0);
        start[0] = start_type;

        if (packets.size() == 1) { // 如果只有一个包就再加一个
            packets.add(PACKET_END);
        }

        // 最少会有两个包啦
        // 保证最后一个包也是20
        byte[] last = packets.get(packets.size() - 1);
        if (last.length < 20) {
            last = ByteUtils.combine2Bytes(last, new byte[size + 1 - last.length]);
            packets.remove(packets.size() - 1);
            packets.add(last);
        }

        // 结束符
        byte[] end = packets.get(packets.size() - 1);
        end[0] = END_DEFAUL;

        return packets;
    }

    public List<byte[]> subpackage(byte[] data) {
        List<byte[]> packets = new ArrayList<>();
        if (data == null || data.length == 0) { // 确保能分包
            return packets;
        }

        int size = 32 * 1024; // 匿名共享内存的一半即可
        int length = data.length;
        int remain = length % size;
        int time = length / size;

        for (int i = 0; i < time; i++) {
            byte[] bytes = Arrays.copyOfRange(data, i * size, (i + 1) * size);
            bytes = ByteUtils.combine2Bytes(PACKET_DEFAULT, bytes);
            packets.add(bytes);
        }
        if (remain > 0) {
            byte[] bytes = Arrays.copyOfRange(data, length - remain, length);
            bytes = ByteUtils.combine2Bytes(PACKET_DEFAULT, bytes);
            packets.add(bytes);
        }

        byte[] start = packets.get(0);
        start[0] = START_DEFAULT;

        if (packets.size() == 1) { // 如果只有一个包就再加一个
            packets.add(PACKET_END);
        }

        byte[] end = packets.get(packets.size() - 1); // 最少会有两个包啦
        end[0] = END_DEFAUL;

        return packets;
    }
}
