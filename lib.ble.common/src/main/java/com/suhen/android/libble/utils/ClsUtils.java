package com.suhen.android.libble.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("JavaReflectionMemberAccess")
@SuppressLint("PrivateApi")
public class ClsUtils {
    private static final String DEFAULT_MAC_ADDRESS_1 = "02:00:00:00:00:00";
    private static final String DEFAULT_MAC_ADDRESS_2 = "00:00:46:66:30:01";

    @SuppressLint("HardwareIds")
    public static String getBtAddress() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String address = bluetoothAdapter.getAddress(); // step 1

        if (isValidateAddress(address)) {
            return address;

        } else {
            address = getBluetoothAddressSdk23(); // step 2

            if (isValidateAddress(address)) {
                return address;

            } else {
                address = getBtAddressViaReflection(); // step 3

                if (isValidateAddress(address)) {
                    return address;
                } else {
                    return null;
                }
            }
        }
    }

    private static String getBtAddressViaReflection() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothMacAddress = null;
        try {
            Field mServiceField = bluetoothAdapter.getClass().getDeclaredField("mService");
            mServiceField.setAccessible(true);

            Object btManagerService = mServiceField.get(bluetoothAdapter);

            if (btManagerService != null) {
                bluetoothMacAddress = (String) btManagerService.getClass().getMethod("getAddress")
                        .invoke(btManagerService);
            }
        } catch (Exception ignore) {
        }
        return bluetoothMacAddress;
    }

    private static boolean isValidateAddress(String address) {
        return address != null && !address.equals(DEFAULT_MAC_ADDRESS_1) && !address
                .equals(DEFAULT_MAC_ADDRESS_2) && BluetoothAdapter.checkBluetoothAddress(address);
    }

    private static String getBluetoothAddressSdk23() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Class<? extends BluetoothAdapter> btAdapterClass = adapter.getClass();
        try {
            Class<?> btClass = Class.forName("android.bluetooth.IBluetooth");
            Field bluetooth = btAdapterClass.getDeclaredField("mService");
            bluetooth.setAccessible(true);
            Method btAddress = btClass.getMethod("getAddress");
            btAddress.setAccessible(true);
            return (String) btAddress.invoke(bluetooth.get(adapter));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean pair(String mac) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!BluetoothAdapter.checkBluetoothAddress(mac)) { //检查是否是有效的蓝牙地址
            return false;
        }
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mac);
        if (device != null) {
            boolean returnValue = false;
            try {
                returnValue = ClsUtils.createBond(BluetoothDevice.class, device);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (returnValue) { //发起配对成功,并不代表配对成功，因为可能被拒绝
                return true;
            }
        }
        return false;
    }

    /**
     * 与设备配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/SETTINGS/bluetooth/CachedBluetoothDevice.java
     */
    public static boolean createBond(Class<?> btClass, BluetoothDevice btDevice)
            throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        return (boolean) createBondMethod.invoke(btDevice);
    }

    public static boolean cancelPair(String mac) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!BluetoothAdapter.checkBluetoothAddress(mac)) { //检查是否是有效的蓝牙地址
            return false;
        }
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mac);
        if (device != null) {
            boolean returnValue = false;
            try {
                returnValue = ClsUtils.removeBond(BluetoothDevice.class, device);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return returnValue;
        }
        return false;
    }

    /**
     * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/SETTINGS/bluetooth/CachedBluetoothDevice.java
     */
    public static boolean removeBond(Class<?> btClass, BluetoothDevice btDevice)
            throws Exception {
        Method removeBondMethod = btClass.getMethod("removeBond");
        return (boolean) removeBondMethod.invoke(btDevice);
    }

    public static boolean setPin(
            Class<? extends BluetoothDevice> btClass, BluetoothDevice btDevice,
            String str
    ) throws Exception {
        try {
            Method removeBondMethod = btClass.getDeclaredMethod("setPin", byte[].class);
            Boolean returnValue = (Boolean) removeBondMethod.invoke(
                    btDevice,
                    new Object[]{
                            str.getBytes()}
            );
            Log.e("returnValue", "" + returnValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }

    public static boolean setPassKey(
            Class<? extends BluetoothDevice> btClass,
            BluetoothDevice device,
            int key
    ) {

        Method removeBondMethod = null;
        try {
            removeBondMethod = btClass.getDeclaredMethod(
                    "setPassKey",
                    int.class
            );

            Boolean returnValue = (Boolean) removeBondMethod.invoke(
                    device,
                    key
            );
            Log.e("returnValue", "" + returnValue);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    // 取消用户输入
    public static boolean cancelPairingUserInput(
            Class<?> btClass,
            BluetoothDevice device
    ) throws Exception {
        Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
        //        cancelBondProcess(btClass, device);
        return (boolean) createBondMethod.invoke(device);
    }

    // 取消配对
    public static boolean cancelBondProcess(
            Class<?> btClass,
            BluetoothDevice device
    )

            throws Exception {
        Method createBondMethod = btClass.getMethod("cancelBondProcess");
        return (boolean) createBondMethod.invoke(device);
    }

    //确认配对
    static public void setPairingConfirmation(
            Class<?> btClass, BluetoothDevice device,
            boolean isConfirm
    ) throws Exception {
        Method setPairingConfirmation = btClass.getDeclaredMethod(
                "setPairingConfirmation",
                boolean.class
        );
        setPairingConfirmation.invoke(device, isConfirm);
    }


    /**
     * @param clsShow
     */
    public static void printAllInform(Class<?> clsShow) {
        try {
            // 取得所有方法
            Method[] hideMethod = clsShow.getMethods();
            int i = 0;
            for (; i < hideMethod.length; i++) {
                Log.e("method name", hideMethod[i].getName() + ";and the i is:" + i);
            }
            // 取得所有常量
            Field[] allFields = clsShow.getFields();
            for (i = 0; i < allFields.length; i++) {
                Log.e("Field name", allFields[i].getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setDiscoverableTimeout(int timeout) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod(
                    "setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class,
                    int.class
            );
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, timeout);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE,
                    timeout
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeDiscoverableTimeout() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod(
                    "setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class,
                    int.class
            );
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, 1);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_NONE, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}