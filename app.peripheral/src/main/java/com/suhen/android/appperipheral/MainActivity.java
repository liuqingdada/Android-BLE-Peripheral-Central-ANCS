package com.suhen.android.appperipheral;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.suhen.android.libble.peripheral.BlePeripheral;

public class MainActivity extends AppCompatActivity {

    private BlePeripheral mBlePeripheral;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBlePeripheral = BlePeripheral.getInstance(this);
        mBlePeripheral.onCreate();


        mBlePeripheral.setupPeripheral();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBlePeripheral.onDestroy();
    }
}
