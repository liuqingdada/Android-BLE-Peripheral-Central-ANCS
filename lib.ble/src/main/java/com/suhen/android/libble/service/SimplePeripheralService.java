package com.suhen.android.libble.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SimplePeripheralService extends Service {
    public SimplePeripheralService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
