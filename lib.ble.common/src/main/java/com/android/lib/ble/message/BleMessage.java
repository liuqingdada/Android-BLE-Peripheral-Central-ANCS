package com.android.lib.ble.message;

import com.android.lib.ble.utils.ByteUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 * <p>
 * message:
 * [0xff, ...] [0x00, ...] [0x00, ...] ... [0x01, ...]
 * 除去包头, 其他的数据为有效负载, payload
 */
public abstract class BleMessage {
    ////////////////////////////////////////////////////////////////
    public static final int MTU = 20;
    public static final byte END_DEFAULT = (byte) 0x01;

    public static final byte START_DEFAULT = (byte) 0xff;
    public static final byte START_ONE_PACKAGE_MSG = (byte) 0xfe;

    public static final byte[] PACKET_DEFAULT = {(byte) 0x00};
    public static final byte[] PACKET_END = {END_DEFAULT};
    ////////////////////////////////////////////////////////////////

    private byte[] payload;

    protected abstract byte[] payload();

    public byte[] getPayload() {
        if (payload == null) {
            payload = payload();
        }
        return payload;
    }

    public List<byte[]> subpackage(byte[] data) {
        List<byte[]> packets = new ArrayList<>();
        int size = MTU - 1; // 每一个包的大小 = size + 1

        if (data == null || data.length == 0) { // 确保能分包
            return packets;
        }

        int length = data.length;
        int remain = length % size;
        int time = length / size;

        /*
         * 分包并添加包头
         */
        for (int i = 0; i < time; i++) {
            byte[] bytes = Arrays.copyOfRange(data, i * size, (i + 1) * size);
            bytes = ByteUtil.combine2Bytes(PACKET_DEFAULT, bytes);
            packets.add(bytes);
        }
        if (remain > 0) {
            byte[] bytes = Arrays.copyOfRange(data, length - remain, length);
            bytes = ByteUtil.combine2Bytes(PACKET_DEFAULT, bytes);
            packets.add(bytes);
        }

        byte[] start = packets.get(0);
        start[0] = START_DEFAULT;

        if (packets.size() == 1) {
            start[0] = START_ONE_PACKAGE_MSG;
        } else {
            // 结束符
            byte[] end = packets.get(packets.size() - 1);
            end[0] = END_DEFAULT;
        }

        return packets;
    }
}
