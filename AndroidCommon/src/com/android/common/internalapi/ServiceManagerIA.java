package com.android.common.internalapi;

import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.common.utils.LogUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ServiceManagerIA {
    private static final String TAG = "ServiceManagerIA";

    private static Class<?> sClass_ServiceManager;

    private static Method sMtd_getService;
    private static Method sMtd_checkService;
    private static Method sMtd_addService;
    private static Method sMtd_listServices;

    static {
        try {
            sClass_ServiceManager = Class.forName("android.os.ServiceManager", false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            LogUtil.w(TAG, "class not found", e);
        }
    }

    private ServiceManagerIA() {
        // nothing to do
    }

    @VisibleForTesting
    static Method reflect_getService() {
        if (sMtd_getService != null || sClass_ServiceManager == null) {
            return sMtd_getService;
        }

        try {
            // public static IBinder getService(String name)
            sMtd_getService = sClass_ServiceManager.getMethod("getService", String.class);
        } catch (NoSuchMethodException e) {
            LogUtil.w(TAG, "method not found", e);
        }
        return sMtd_getService;
    }

    /**
     * Returns a reference to a service with the given name.
     *
     * <p>Important: May block the calling thread!</p>
     * @param name the name of the service to get
     * @return a reference to the service, or <code>null</code> if the service doesn't exist
     */
    @Nullable
    public static IBinder getService(@NonNull String name) {
        reflect_getService();
        if (sMtd_getService != null) {
            try {
                return (IBinder) sMtd_getService.invoke(null, name);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                LogUtil.w(TAG, "Failed to invoke #getService()", e);
            }
        } else {
            LogUtil.w(TAG, "#getService() not available");
        }
        return null;
    }

    @VisibleForTesting
    static Method reflect_checkService() {
        if (sMtd_checkService != null || sClass_ServiceManager == null) {
            return sMtd_checkService;
        }

        try {
            // public static IBinder checkService(String name)
            sMtd_checkService = sClass_ServiceManager.getMethod("checkService", String.class);
        } catch (NoSuchMethodException e) {
            LogUtil.w(TAG, "method not found", e);
        }
        return sMtd_checkService;
    }

    /**
     * Retrieve an existing service called @a name from the
     * service manager.  Non-blocking.
     */
    @Nullable
    public static IBinder checkService(@NonNull String name) {
        reflect_checkService();
        if (sMtd_checkService != null) {
            try {
                return (IBinder) sMtd_checkService.invoke(null, name);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                LogUtil.w(TAG, "Failed to invoke #checkService()", e);
            }
        } else {
            LogUtil.w(TAG, "#checkService() not available");
        }
        return null;
    }

    @VisibleForTesting
    static Method reflect_addService() {
        if (sMtd_addService != null || sClass_ServiceManager == null) {
            return sMtd_addService;
        }

        try {
            // public static void addService(String name, IBinder service)
            sMtd_addService = sClass_ServiceManager.getMethod("addService",
                    String.class, IBinder.class);
        } catch (NoSuchMethodException e) {
            LogUtil.w(TAG, "method not found", e);
        }
        return sMtd_addService;
    }

    /**
     * Place a new @a service called @a name into the service
     * manager.
     *
     * @param name the name of the new service
     * @param service the service object
     */
    public static void addService(@NonNull String name, @NonNull IBinder service) {
        reflect_addService();
        if (sMtd_addService != null) {
            try {
                sMtd_addService.invoke(null, name, service);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                LogUtil.w(TAG, "Failed to invoke #addService()", e);
            }
        } else {
            LogUtil.w(TAG, "#addService() not available");
        }
    }

    @VisibleForTesting
    static Method reflect_listServices() {
        if (sMtd_listServices != null || sClass_ServiceManager == null) {
            return sMtd_listServices;
        }

        try {
            // public static String[] listServices() throws RemoteException
            sMtd_listServices = sClass_ServiceManager.getMethod("listServices");
        } catch (NoSuchMethodException e) {
            LogUtil.w(TAG, "method not found", e);
        }
        return sMtd_listServices;
    }

    /**
     * Return a list of all currently running services.
     */
    @Nullable
    public static String[] listServices() {
        reflect_listServices();
        if (sMtd_listServices != null) {
            try {
                return (String[]) sMtd_listServices.invoke(null);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                LogUtil.w(TAG, "Failed to invoke #listServices()", e);
            }
        } else {
            LogUtil.w(TAG, "#listServices() not available");
        }
        return null;
    }
}
