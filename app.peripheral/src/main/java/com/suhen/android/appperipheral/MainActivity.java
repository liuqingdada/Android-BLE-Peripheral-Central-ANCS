package com.suhen.android.appperipheral;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.suhen.android.appperipheral.service.SimpleBlePeripheralService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ContextCompat.startForegroundService(this, new Intent(this, SimpleBlePeripheralService.class));
    }
}
