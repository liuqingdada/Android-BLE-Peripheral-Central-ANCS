package com.android.lib.ble.central

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import androidx.annotation.IntDef

/**
 * @author: cooper
 * Date: 20-10-22.
 * Email: yangliuqing@xiaomi.com
 */
interface ICentral {
    companion object {
        const val UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR =
            "00002902-0000-1000-8000-00805f9b34fb"
    }

    val device: BluetoothDevice

    @BtProfileState
    var lastState: Int

    var centralGattCallback: CentralGattCallback?

    var rssiOperator: CentralGattOperator?

    var mtuOperator: CentralGattOperator?

    fun connect(autoConnect: Boolean)

    fun disconnect()

    fun addOperator(operator: CentralGattOperator)

    fun removeOperator(operator: CentralGattOperator)

    fun readCharacter(operator: CentralGattOperator)

    fun writeCharacter(operator: CentralGattOperator, data: ByteArray)

    fun notifyCharacter(
        operator: CentralGattOperator,
        enable: Boolean,
        useCharacterDescriptor: Boolean = false
    )

    fun indicateCharacter(
        operator: CentralGattOperator,
        enable: Boolean,
        useCharacterDescriptor: Boolean = false
    )

    fun readRemoteRssi()

    fun setMtu(mtu: Int)
}

interface CentralGattCallback {
    companion object {
        const val STATUS_GATT_NULL = 100
    }

    fun onStartConnect(gatt: BluetoothGatt)

    fun onConnectFailed(status: Int)

    fun onConnected(gatt: BluetoothGatt, status: Int)

    fun onDisconnected(gatt: BluetoothGatt, status: Int)

    fun onServicesDiscovered(gatt: BluetoothGatt, status: Int)

    fun onServicesDiscoverFailed(gatt: BluetoothGatt, status: Int)
}

abstract class CentralGattOperator(val serviceUUID: String, val characterUUID: String) {
    open fun onRead(value: ByteArray, status: Int) {}

    open fun onWrite(value: ByteArray, status: Int) {}

    open fun onChange(value: ByteArray) {}

    open fun onReadRemoteRssi(rssi: Int, status: Int) {}

    open fun onMtuChanged(mtu: Int, status: Int) {}

    open fun operateFailed() {}
}

@IntDef(
    BluetoothProfile.STATE_DISCONNECTED,
    BluetoothProfile.STATE_CONNECTING,
    BluetoothProfile.STATE_CONNECTED,
    BluetoothProfile.STATE_DISCONNECTING
)
@Retention(AnnotationRetention.SOURCE)
annotation class BtProfileState
