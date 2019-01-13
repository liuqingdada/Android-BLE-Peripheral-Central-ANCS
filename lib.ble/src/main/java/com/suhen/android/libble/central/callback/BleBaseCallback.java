package com.suhen.android.libble.central.callback;

/**
 * Created by yangliuqing
 * 2019/1/13.
 * Email: 1239604859@qq.com
 */
public abstract class BleBaseCallback {

    private String mUUID;

    public BleBaseCallback(String UUID) {
        mUUID = UUID;
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

    public String getUUID() {
        return mUUID;
    }
}
