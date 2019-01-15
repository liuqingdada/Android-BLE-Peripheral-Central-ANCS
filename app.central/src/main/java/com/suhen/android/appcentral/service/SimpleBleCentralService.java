package com.suhen.android.appcentral.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.suhen.android.appcentral.R;
import com.suhen.android.libble.BLE;
import com.suhen.android.libble.central.ICentral;
import com.suhen.android.libble.permission.PermissionWizard;
import com.suhen.android.libble.simple.NotificationWizard;

public class SimpleBleCentralService extends Service {
    private static final String TAG = SimpleBleCentralService.class.getSimpleName();
    private static final int FOREGROUND_ID = 0xFFFE;
    private ICentral mCentral;
    private BleCentralBinder mBinder;

    public SimpleBleCentralService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new BleCentralBinder();
        }
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        startForegroound();

        try {
            mCentral = BLE.newCentral(BleCentralImplBle.class, this);

            mCentral.onCreate();

            mCentral.setup();

        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean locationEnable = PermissionWizard.isLocationEnable(this);
        Log.d(TAG, "onCreate: locationEnable = " + locationEnable);
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

    public class BleCentralBinder extends Binder {
        public ICentral getBleCentral() {
            return mCentral;
        }
    }
}
