package com.suhen.android.libble.permission;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Created by yangliuqing
 * 2019/1/14.
 * Email: 1239604859@qq.com
 */
public class PermissionWizard {

    private PermissionWizard() {
    }

    /**
     * Check if the caller has been granted a set of permissions.
     *
     * @return true if all permissions are already granted,
     * false if at least one permission is not yet granted.
     */
    public static boolean hasPermissions(
            @NonNull Context cxt,
            @NonNull String... permissions
    ) {
        // At least one permission must be checked.
        if (permissions.length < 1) {
            return false;
        }

        boolean result = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(cxt, perm)
                    == PackageManager.PERMISSION_DENIED) {
                result = false;
            }
        }
        return result;
    }

    public static boolean verifyPermissions(@NonNull int[] grantResults) {
        // At least one result must be checked.
        if (grantResults.length < 1) {
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    public static boolean isLocationEnable(Context context) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return networkProvider || gpsProvider;
    }

    public static boolean isLocationServiceAllowed(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int locationMode = Settings.Secure.LOCATION_MODE_OFF;
            try {
                locationMode = Settings.Secure.getInt(
                        context.getContentResolver(),
                        Settings.Secure.LOCATION_MODE
                );
            } catch (final Settings.SettingNotFoundException e) {
                // do nothing
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        return true;
    }
}
