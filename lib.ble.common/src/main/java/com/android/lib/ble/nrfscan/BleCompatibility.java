package com.android.lib.ble.nrfscan;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

import com.android.common.utils.LogUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by andy
 * 2019-08-20.
 * Email: 1239604859@qq.com
 */
@SuppressWarnings("all")
public class BleCompatibility {
    private static final String TAG = "BleCompatibility";

    /**
     * 某些手机用久了会出现扫描不到任何设备的bug
     *
     * @param context
     * @param packageName
     */
    public static void refreshBleAppFromSystem(Context context, String packageName) {
        LogUtil.d(TAG, "call refreshBleAppFromSystem: ");
        // 6.0以上才有该功能,不是6.0以上就算了
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        if (!adapter.isEnabled()) {
            return;
        }
        try {
            Object mIBluetoothManager = getIBluetoothManager(adapter);
            if (mIBluetoothManager == null) {
                return;
            }
            Method isBleAppPresentM =
                    mIBluetoothManager.getClass().getDeclaredMethod("isBleAppPresent");
            isBleAppPresentM.setAccessible(true);
            boolean isBleAppPresent = (Boolean) isBleAppPresentM.invoke(mIBluetoothManager);
            if (isBleAppPresent) {
                return;
            }
            Field mIBinder = BluetoothAdapter.class.getDeclaredField("mToken");
            mIBinder.setAccessible(true);
            Object mToken = mIBinder.get(adapter);

            //刷新偶尔系统无故把app视为非 BLE应用 的错误标识 导致无法扫描设备
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //8.0+ (部分手机是7.1.2 也是如此)
                Method updateBleAppCount = mIBluetoothManager.getClass().getDeclaredMethod(
                        "updateBleAppCount",
                        IBinder.class,
                        boolean.class,
                        String.class
                );
                updateBleAppCount.setAccessible(true);
                //关一下 再开
                updateBleAppCount.invoke(mIBluetoothManager, mToken, false, packageName);
                updateBleAppCount.invoke(mIBluetoothManager, mToken, true, packageName);

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    // 6.0~7.1.1
                    Method updateBleAppCount = mIBluetoothManager.getClass().getDeclaredMethod(
                            "updateBleAppCount",
                            IBinder.class,
                            boolean.class
                    );
                    updateBleAppCount.setAccessible(true);
                    //关一下 再开
                    updateBleAppCount.invoke(mIBluetoothManager, mToken, false);
                    updateBleAppCount.invoke(mIBluetoothManager, mToken, true);
                } catch (NoSuchMethodException e) {
                    //8.0+ (部分手机是7.1.2 也是如此)
                    try {
                        Method updateBleAppCount = mIBluetoothManager.getClass().getDeclaredMethod(
                                "updateBleAppCount",
                                IBinder.class,
                                boolean.class,
                                String.class
                        );
                        updateBleAppCount.setAccessible(true);
                        //关一下 再开
                        updateBleAppCount.invoke(mIBluetoothManager, mToken, false, packageName);
                        updateBleAppCount.invoke(mIBluetoothManager, mToken, true, packageName);
                    } catch (NoSuchMethodException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } catch (Throwable e) {
            LogUtil.e(TAG, "refreshBleAppFromSystem: ", e);
        }
    }

    public static final int CONNECTION_STATE_DISCONNECTED = 0;
    public static final int CONNECTION_STATE_CONNECTED = 1;
    public static final int CONNECTION_STATE_UN_SUPPORT = -1;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("PrivateApi")
    public static int getInternalConnectionState(String mac) {
        // 该功能是在21(5.1.0)以上才支持, 5.0 以及以下 都 不支持
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return CONNECTION_STATE_UN_SUPPORT;
        }
        if (Build.MANUFACTURER.equalsIgnoreCase("OPPO")) { //OPPO勿使用这种办法判断, OPPO无解
            return CONNECTION_STATE_UN_SUPPORT;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice remoteDevice = adapter.getRemoteDevice(mac);
        Object mIBluetooth = null;
        try {
            Field sService = BluetoothDevice.class.getDeclaredField("sService");
            sService.setAccessible(true);
            mIBluetooth = sService.get(null);
        } catch (Exception e) {
            LogUtil.e(TAG, "getInternalConnectionState: ", e);
            return CONNECTION_STATE_UN_SUPPORT;
        }
        if (mIBluetooth == null) return CONNECTION_STATE_UN_SUPPORT;

        boolean isConnected;
        try {
            Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected");
            isConnectedMethod.setAccessible(true);
            isConnected = (Boolean) isConnectedMethod.invoke(remoteDevice);
            isConnectedMethod.setAccessible(false);
        } catch (Exception e) {
            LogUtil.e(TAG, "getInternalConnectionState: ", e);
            // 如果找不到,说明不兼容isConnected, 尝试去使用getConnectionState判断
            try {
                Method getConnectionState = mIBluetooth.getClass()
                        .getDeclaredMethod("getConnectionState", BluetoothDevice.class);
                getConnectionState.setAccessible(true);
                int state = (Integer) getConnectionState.invoke(mIBluetooth, remoteDevice);
                getConnectionState.setAccessible(false);
                isConnected = state == CONNECTION_STATE_CONNECTED;
            } catch (Exception e1) {
                LogUtil.e(TAG, "getInternalConnectionState: ", e1);
                return CONNECTION_STATE_UN_SUPPORT;
            }
        }
        return isConnected ? CONNECTION_STATE_CONNECTED : CONNECTION_STATE_DISCONNECTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setLeServiceEnable(boolean isEnable) {
        Object mIBluetooth;
        try {
            Field sService = BluetoothDevice.class.getDeclaredField("sService");
            sService.setAccessible(true);
            mIBluetooth = sService.get(null);
        } catch (Exception e) {
            LogUtil.e(TAG, "setLeServiceEnable: ", e);
            return;
        }
        if (mIBluetooth == null) return;

        try {
            if (isEnable) {
                Method onLeServiceUp = mIBluetooth.getClass().getDeclaredMethod("onLeServiceUp");
                onLeServiceUp.setAccessible(true);
                onLeServiceUp.invoke(mIBluetooth);
            } else {
                Method onLeServiceUp = mIBluetooth.getClass().getDeclaredMethod("onBrEdrDown");
                onLeServiceUp.setAccessible(true);
                onLeServiceUp.invoke(mIBluetooth);
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "setLeServiceEnable: ", e);
        }
    }

    /**
     * 多次打开app/退出app/后台被杀等, 导致扫描不到设备,
     * 并返回 ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED 错误
     *
     * @return
     */
    public static boolean releaseAllScanClient() {
        LogUtil.d(TAG, "call releaseAllScanClient: ");
        try {
            Object mIBluetoothManager = getIBluetoothManager(BluetoothAdapter.getDefaultAdapter());
            if (mIBluetoothManager == null) return false;
            Object iGatt = getIBluetoothGatt(mIBluetoothManager);
            if (iGatt == null) return false;

            Method unregisterClient;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                unregisterClient = getDeclaredMethod(iGatt, "unregisterScanner", int.class);
            } else {
                unregisterClient = getDeclaredMethod(iGatt, "unregisterClient", int.class);
            }
            Method stopScan;
            int type;
            try {
                type = 0;
                stopScan = getDeclaredMethod(iGatt, "stopScan", int.class, boolean.class);
            } catch (Exception e) {
                type = 1;
                stopScan = getDeclaredMethod(iGatt, "stopScan", int.class);
            }

            for (int mClientIf = 0; mClientIf <= 40; mClientIf++) {
                if (type == 0) {
                    try {
                        stopScan.invoke(iGatt, mClientIf, false);
                    } catch (Exception ignored) {
                    }
                }
                if (type == 1) {
                    try {
                        stopScan.invoke(iGatt, mClientIf);
                    } catch (Exception ignored) {
                    }
                }
                try {
                    unregisterClient.invoke(iGatt, mClientIf);
                } catch (Exception ignored) {
                }
            }
            stopScan.setAccessible(false);
            unregisterClient.setAccessible(false);
            getDeclaredMethod(iGatt, "unregAll").invoke(iGatt);

            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, "releaseAllScanClient: ", e);
        }
        return false;
    }

    public static boolean releaseScanClient(int scannerId) {
        LogUtil.d(TAG, "call releaseScanClient: ");
        try {
            Object mIBluetoothManager = getIBluetoothManager(BluetoothAdapter.getDefaultAdapter());
            if (mIBluetoothManager == null) return false;
            Object iGatt = getIBluetoothGatt(mIBluetoothManager);
            if (iGatt == null) return false;

            Method unregisterClient;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                unregisterClient = getDeclaredMethod(iGatt, "unregisterScanner", int.class);
            } else {
                unregisterClient = getDeclaredMethod(iGatt, "unregisterClient", int.class);
            }

            Method stopScan;
            int type;
            try {
                type = 0;
                stopScan = getDeclaredMethod(iGatt, "stopScan", int.class, boolean.class);
            } catch (Exception e) {
                type = 1;
                stopScan = getDeclaredMethod(iGatt, "stopScan", int.class);
            }

            if (type == 0) {
                try {
                    stopScan.invoke(iGatt, scannerId, false);
                } catch (Exception ignored) {
                }
            }
            if (type == 1) {
                try {
                    stopScan.invoke(iGatt, scannerId);
                } catch (Exception ignored) {
                }
            }
            try {
                unregisterClient.invoke(iGatt, scannerId);
            } catch (Exception ignored) {
            }

            stopScan.setAccessible(false);
            unregisterClient.setAccessible(false);

            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, "releaseScanClient: ", e);
        }
        return false;
    }

    /**
     * @param callback android le scan callback
     * @return mScannerId or mClientIf
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static int getScanClientId(ScanCallback callback) {
        int mClientIf = -1;
        try {
            Field mLeScanClientsField =
                    getDeclaredField(BluetoothLeScanner.class, "mLeScanClients");
            mLeScanClientsField.setAccessible(true);
            //  HashMap<ScanCallback, BleScanCallbackWrapper>()
            HashMap callbackList = (HashMap) mLeScanClientsField
                    .get(BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner());
            int size = callbackList == null ? 0 : callbackList.size();
            if (size > 0) {
                Iterator iterator = callbackList.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    Object key = entry.getKey();
                    Object val = entry.getValue();
                    if (val != null && key != null && key == callback) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Field mScannerIdField = getDeclaredField(val, "mScannerId");
                            mClientIf = mScannerIdField.getInt(val);
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Field mClientIfField = getDeclaredField(val, "mClientIf");
                            mClientIf = mClientIfField.getInt(val);
                        }
                        LogUtil.d(TAG, "mClientIf=" + mClientIf);
                        return mClientIf;
                    }
                }
            } else {
                if (callback != null) {
                    return mClientIf;
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "isScanClientInitialize: ", e);
        }
        return mClientIf;
    }

    private static Object getIBluetoothGatt(Object mIBluetoothManager)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method getBluetoothGatt = getDeclaredMethod(mIBluetoothManager, "getBluetoothGatt");
        getBluetoothGatt.setAccessible(true);
        return getBluetoothGatt.invoke(mIBluetoothManager);
    }

    private static Object getIBluetoothManager(BluetoothAdapter adapter)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method getBluetoothManager =
                getDeclaredMethod(BluetoothAdapter.class, "getBluetoothManager");
        getBluetoothManager.setAccessible(true);
        return getBluetoothManager.invoke(adapter);
    }

    private static Field getDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException {
        Field declaredField = clazz.getDeclaredField(name);
        declaredField.setAccessible(true);
        return declaredField;
    }

    private static Method getDeclaredMethod(
            Class<?> clazz,
            String name,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        Method declaredMethod = clazz.getDeclaredMethod(name, parameterTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod;
    }

    private static Field getDeclaredField(Object obj, String name) throws NoSuchFieldException {
        Field declaredField = obj.getClass().getDeclaredField(name);
        declaredField.setAccessible(true);
        return declaredField;
    }


    private static Method getDeclaredMethod(
            Object obj,
            String name,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        Method declaredMethod = obj.getClass().getDeclaredMethod(name, parameterTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod;
    }
}
