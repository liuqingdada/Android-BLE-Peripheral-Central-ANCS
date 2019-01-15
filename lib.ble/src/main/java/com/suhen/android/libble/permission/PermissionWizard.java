package com.suhen.android.libble.permission;

import android.content.Context;
import android.location.LocationManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.suhen.android.libble.R;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.io.File;
import java.util.List;

/**
 * Created by yangliuqing
 * 2019/1/14.
 * Email: 1239604859@qq.com
 */
public class PermissionWizard {

    private PermissionWizard() {
    }

    /**
     * Request permissions.
     */
    public static void requestPermission(Context context, PermissionCallback callback, String... permissions) {
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
        String message = context.getString(R.string.message_permission_always_failed, TextUtils.join("\n", permissionNames));

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
                .onComeback(() -> {
//                        Toast.makeText(context, R.string.message_setting_comeback, Toast.LENGTH_SHORT).show();
                })
                .start();
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

    public static void requestPermissionForAlertWindow(Context context, PermissionCallback callback) {
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

    private static boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return networkProvider || gpsProvider;
    }
}
