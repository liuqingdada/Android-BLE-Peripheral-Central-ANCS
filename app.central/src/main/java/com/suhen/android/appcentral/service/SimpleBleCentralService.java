package com.suhen.android.appcentral.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.suhen.android.appcentral.R;
import com.suhen.android.libble.BLE;
import com.suhen.android.libble.central.ICentral;
import com.suhen.android.libble.simple.NotificationWizard;
import com.suhen.android.libble.simple.SimpleBleCentral;

public class SimpleBleCentralService extends Service {
    private static final String TAG = SimpleBleCentralService.class.getSimpleName();
    private static final int FOREGROUND_ID = 0xFFFE;
    private ICentral mCentral;

    public SimpleBleCentralService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        startForegroound();


        try {
            mCentral = BLE.getCentral(SimpleBleCentral.class, this);

            mCentral.onCreate();

            mCentral.setup();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        return START_STICKY;
    }

    private void startForegroound() {
        Log.d(TAG, "startForeground: ");

        startForeground(FOREGROUND_ID, NotificationWizard.generateNotification(
                this, NotificationManagerCompat.IMPORTANCE_DEFAULT, R.mipmap.ic_launcher,
                getClass(), 1));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        mCentral.onDestroy();
    }
}
