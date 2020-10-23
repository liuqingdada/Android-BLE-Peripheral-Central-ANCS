package com.suhen.android.libble.permission;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

/**
 * Created by liuqing
 * 2018/7/31.
 * Email: suhen0420@163.com
 */
public class NotificationWizard {

    public static Notification generateNotification(
            Context context,
            int importance,
            int icon,
            Class<?> clazz,
            int requestCode
    ) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            throw new NullPointerException();
        }
        String appname = getAppName(context);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(appname, appname));

            NotificationChannel channel = new NotificationChannel(appname, appname, importance);
            channel.enableLights(true);
            channel.enableVibration(true);

            channel.setLightColor(Color.RED);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            channel.setShowBadge(true);
            channel.setBypassDnd(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400});
            channel.setDescription(appname);
            channel.setGroup(appname);
            // channel.setSound();

            notificationManager.createNotificationChannel(channel);
        }


        Intent notificationIntent = new Intent(context.getApplicationContext(), clazz);
        PendingIntent pendingIntent = PendingIntent.getService(
                context.getApplicationContext(),
                requestCode,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, appname)
                .setSmallIcon(icon)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(appname)
                .setContentText("")
                .setChannelId(appname)
                .setContentIntent(pendingIntent);
        return builder.build();
    }

    private static String getAppName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(context.getPackageName(), 0);
            return info.loadLabel(pm)
                    .toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

        }
        return context.getPackageName();
    }
}
