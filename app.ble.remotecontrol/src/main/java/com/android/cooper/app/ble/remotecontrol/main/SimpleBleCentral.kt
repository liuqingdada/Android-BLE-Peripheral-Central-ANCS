package com.android.cooper.app.ble.remotecontrol.main

import android.bluetooth.BluetoothDevice
import com.suhen.android.libble.central.BleCentral

/**
 * Created by cooper
 * 20-10-28.
 * Email: 1239604859@qq.com
 */
class SimpleBleCentral(override val device: BluetoothDevice) : BleCentral(device)