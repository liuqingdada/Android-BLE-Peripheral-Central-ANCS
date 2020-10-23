package com.suhen.android.libble.permission;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.suhen.android.libble.R;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.io.File;
import java.util.List;

/**
 * Created by yangliuqing
 * 2019/1/14.
 * Email: 1239604859@qq.com
 */
public class PermissionWizard {
    private static final int HANDLE_DENIED_CODE = 1023;

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

    /**
     * Request permissions.
     */
    @SuppressLint("WrongConstant")
    public static void requestPermission(
            Context context,
            PermissionCallback callback,
            String... permissions
    ) {
        AndPermission.with(context)
                .runtime()
                .permission(permissions)
                .rationale(new RuntimeRationale())
                .onGranted(pl -> callback.onGranted())
                .onDenied(pl -> {
                    callback.onDenied();
                    handleDenied(context, pl);
                })
                .start();
    }

    private static void handleDenied(Context context, List<String> permissions) {
        if (AndPermission.hasAlwaysDeniedPermission(context, permissions)) {
            showSettingDialog(context, permissions);
        }
    }

    /**
     * Display setting dialog.
     */
    private static void showSettingDialog(Context context, final List<String> permissions) {
        List<String> permissionNames = Permission.transformText(context, permissions);
        String message = context.getString(
                R.string.message_permission_always_failed,
                TextUtils.join("\n", permissionNames)
        );

        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.title_dialog)
                .setMessage(message)
                .setPositiveButton(R.string.setting, (dialog, which) -> setPermission(context))
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .show();
    }

    /**
     * Set permissions.
     */
    private static void setPermission(Context context) {
        AndPermission.with(context)
                .runtime()
                .setting()
//                .onComeback(() -> {
//                        Toast.makeText(context, R.string.message_setting_comeback, Toast.LENGTH_SHORT).show();
//                })
                .start(HANDLE_DENIED_CODE);
    }

    /**
     * Install package.
     */
    public static void installPackage(Context context, PermissionCallback callback, File apkFile) {
        AndPermission.with(context)
                .install()
                .file(apkFile)
                .rationale(new InstallRationale())
                .onGranted(data -> {
                    // Installing.
                    callback.onGranted();
                })
                .onDenied(data -> {
                    // The user refused to install.
                    callback.onDenied();
                })
                .start();
    }

    public static void requestPermissionForAlertWindow(
            Context context,
            PermissionCallback callback
    ) {
        AndPermission.with(context)
                .overlay()
                .rationale(new OverlayRationale())
                .onGranted(data -> callback.onGranted())
                .onDenied(data -> callback.onDenied())
                .start();
    }

    public static abstract class PermissionCallback {
        public abstract void onGranted();

        public void onDenied() {
        }
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
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
