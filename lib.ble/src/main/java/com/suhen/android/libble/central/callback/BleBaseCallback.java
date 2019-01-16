package com.suhen.android.libble.central.callback;

/**
 * Created by yangliuqing
 * 2019/1/13.
 * Email: 1239604859@qq.com
 */
public abstract class BleBaseCallback {
    private String mParentUuid;
    private String mChildUuid;

    protected BleBaseCallback(String parentUuid, String childUuid) {
        mParentUuid = parentUuid;
        mChildUuid = childUuid;
    }

    /**
     * read data from peripheral
     */
    public void onCharacteristicRead(byte[] value, int status) {
    }

    /**
     * in indicate, this method will be call when send data to peripheral
     * in notify, this method can ignore
     */
    public void onCharacteristicWrite(byte[] value, int status) {
    }

    /**
     * peripheral send data to me;
     * notify or indicate
     */
    public void onCharacteristicChanged(byte[] value) {
    }

    /**
     * like the {@link BleBaseCallback#onCharacteristicRead}
     */
    public void onDescriptorRead(byte[] value, int status) {
    }

    public void onDescriptorWrite(byte[] value, int status) {
    }

    public void onReadRemoteRssi(int rssi, int status) {
    }

    public void onMtuChanged(int mtu, int status) {
    }

    public String getParentUuid() {
        return mParentUuid;
    }

    public String getChildUuid() {
        return mChildUuid;
    }
}
