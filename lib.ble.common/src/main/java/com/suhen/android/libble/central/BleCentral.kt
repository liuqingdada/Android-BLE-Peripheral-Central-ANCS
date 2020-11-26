package com.suhen.android.libble.central

import android.bluetooth.*
import android.bluetooth.BluetoothDevice.PHY_LE_1M_MASK
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import com.android.common.utils.AppUtils
import com.android.common.utils.LogUtil
import com.suhen.android.libble.central.ICentral.Companion.UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author: cooper
 * Date: 20-10-22.
 * Email: yangliuqing@xiaomi.com
 */
abstract class BleCentral(override val device: BluetoothDevice) : ICentral {
    companion object {
        private const val TAG = "BleCentral"
    }

    @BtProfileState
    @Volatile
    override var lastState = BluetoothProfile.STATE_DISCONNECTED

    @Volatile
    override var centralGattCallback: CentralGattCallback? = null

    override var rssiOperator: CentralGattOperator? = null

    override var mtuOperator: CentralGattOperator? = null

    @Volatile
    private var bluetoothGatt: BluetoothGatt? = null

    private val context: Context by lazy {
        AppUtils.application
    }

    private val handlerThread by lazy {
        val t = HandlerThread("blec-central-loop-thread")
        t.start()
        t
    }

    private val handler = Handler(handlerThread.looper)

    private val operators = ConcurrentLinkedQueue<CentralGattOperator>()

    private var isServicesDiscovered = false

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            handler.post {
                LogUtil.d(TAG, "onConnectionStateChange, status: $status, newState: $newState")
                if (gatt != null) {
                    bluetoothGatt = gatt
                } else {
                    LogUtil.d(TAG, "system return gatt null in onConnectionStateChange")
                }
                when (newState) {
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        lastState = BluetoothProfile.STATE_DISCONNECTED
                        isServicesDiscovered = false

                        disconnectGatt()
                        refreshDeviceCache()
                        closeBluetoothGatt()

                        centralGattCallback?.onDisconnected(bluetoothGatt!!, status)
                    }
                    BluetoothProfile.STATE_CONNECTED -> {
                        lastState = BluetoothProfile.STATE_CONNECTED
                        centralGattCallback?.onConnected(bluetoothGatt!!, status)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            handler.post {
                LogUtil.d(TAG, "onServicesDiscovered: $status")
                if (gatt != null) {
                    bluetoothGatt = gatt
                } else {
                    LogUtil.d(TAG, "system return gatt null in onServicesDiscovered")
                }
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    isServicesDiscovered = true
                    centralGattCallback?.onServicesDiscovered(bluetoothGatt!!, status)
                } else {
                    isServicesDiscovered = false
                    centralGattCallback?.onServicesDiscoverFailed(bluetoothGatt!!, status)
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            handler.post {
                operators.forEach {
                    if (it.serviceUUID == characteristic?.service?.uuid?.toString() &&
                        it.characterUUID == characteristic.uuid.toString()
                    ) {
                        it.onRead(characteristic.value, status)
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            handler.post {
                operators.forEach {
                    if (it.serviceUUID == characteristic?.service?.uuid?.toString() &&
                        it.characterUUID == characteristic.uuid.toString()
                    ) {
                        it.onWrite(characteristic.value, status)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            handler.post {
                operators.forEach {
                    if (it.serviceUUID == characteristic?.service?.uuid?.toString() &&
                        it.characterUUID == characteristic.uuid.toString()
                    ) {
                        it.onChange(characteristic.value)
                    }
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            handler.post {
                rssiOperator?.onReadRemoteRssi(rssi, status)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            handler.post {
                mtuOperator?.onMtuChanged(mtu, status)
            }
        }
    }

    override fun connect(autoConnect: Boolean) {
        val gatt = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                device.connectGatt(context, autoConnect, gattCallback, TRANSPORT_LE, PHY_LE_1M_MASK)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                device.connectGatt(context, autoConnect, gattCallback, TRANSPORT_LE)
            }
            else -> {
                device.connectGatt(context, autoConnect, gattCallback)
            }
        }
        if (gatt == null) {
            disconnectGatt()
            refreshDeviceCache()
            closeBluetoothGatt()
            lastState = BluetoothProfile.STATE_DISCONNECTED
            centralGattCallback?.onConnectFailed(CentralGattCallback.STATUS_GATT_NULL)
        } else {
            lastState = BluetoothProfile.STATE_CONNECTING
            bluetoothGatt = gatt
            centralGattCallback?.onStartConnect(gatt)
        }
    }

    override fun disconnect() {
        disconnectGatt()
    }

    override fun addOperator(operator: CentralGattOperator) {
        operators.add(operator)
    }

    override fun removeOperator(operator: CentralGattOperator) {
        operators.remove(operator)
    }

    override fun readCharacter(operator: CentralGattOperator) {
        if (checkService()) {
            val service = bluetoothGatt?.getService(UUID.fromString(operator.serviceUUID))
            val character = service?.getCharacteristic(UUID.fromString(operator.characterUUID))
            if (service == null || character == null) {
                LogUtil.d(TAG, "Gatt service is not exist")
                operator.operateFailed()
            } else {
                if (character.properties and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                    val flag = bluetoothGatt?.readCharacteristic(character)
                    if (flag != true) {
                        LogUtil.d(TAG, "Gatt readCharacteristic failed")
                        operator.operateFailed()
                        return
                    }
                } else {
                    LogUtil.d(TAG, "Gatt this characteristic not support read")
                    operator.operateFailed()
                    return
                }
            }
        } else {
            operator.operateFailed()
        }
    }

    override fun writeCharacter(operator: CentralGattOperator, data: ByteArray) {
        if (checkService()) {
            val service = bluetoothGatt?.getService(UUID.fromString(operator.serviceUUID))
            val character = service?.getCharacteristic(UUID.fromString(operator.characterUUID))
            if (service == null || character == null) {
                LogUtil.d(TAG, "Gatt service is not exist")
                operator.operateFailed()
            } else {
                if (character.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
                    LogUtil.d(TAG, "Gatt this characteristic not support write")
                    operator.operateFailed()
                    return
                }
                character.value = data
                val flag = bluetoothGatt?.writeCharacteristic(character)
                if (flag != true) {
                    LogUtil.d(TAG, "Gatt writeCharacteristic failed")
                    operator.operateFailed()
                    return
                }
            }
        } else {
            operator.operateFailed()
        }
    }

    override fun notifyCharacter(
        operator: CentralGattOperator,
        enable: Boolean,
        useCharacterDescriptor: Boolean
    ) {
        if (checkService()) {
            val service = bluetoothGatt?.getService(UUID.fromString(operator.serviceUUID))
            val character = service?.getCharacteristic(UUID.fromString(operator.characterUUID))
            if (service == null || character == null) {
                LogUtil.d(TAG, "Gatt service is not exist")
                operator.operateFailed()
            } else {
                if (character.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    val flag1 = bluetoothGatt?.setCharacteristicNotification(character, enable)
                    if (flag1 != true) {
                        LogUtil.d(TAG, "Gatt setCharacteristicNotification false")
                        operator.operateFailed()
                        return
                    }
                    val descriptor = if (useCharacterDescriptor) {
                        character.getDescriptor(character.uuid)
                    } else {
                        character.getDescriptor(
                            UUID.fromString(
                                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
                            )
                        )
                    }
                    if (descriptor == null) {
                        LogUtil.d(TAG, "Gatt characteristic's descriptor is null")
                        operator.operateFailed()
                        return
                    }
                    descriptor.value = if (enable) {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }
                    val flag2 = bluetoothGatt?.writeDescriptor(descriptor)
                    if (flag2 != true) {
                        LogUtil.d(TAG, "Gatt writeDescriptor false")
                        operator.operateFailed()
                        return
                    }
                } else {
                    LogUtil.d(TAG, "Gatt this characteristic not support notify")
                    operator.operateFailed()
                    return
                }
            }
        } else {
            operator.operateFailed()
        }
    }

    override fun indicateCharacter(
        operator: CentralGattOperator,
        enable: Boolean,
        useCharacterDescriptor: Boolean
    ) {
        if (checkService()) {
            val service = bluetoothGatt?.getService(UUID.fromString(operator.serviceUUID))
            val character = service?.getCharacteristic(UUID.fromString(operator.characterUUID))
            if (service == null || character == null) {
                LogUtil.d(TAG, "Gatt service is not exist")
                operator.operateFailed()
            } else {
                if (character.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    val flag1 = bluetoothGatt?.setCharacteristicNotification(character, enable)
                    if (flag1 != true) {
                        LogUtil.d(TAG, "Gatt setCharacteristicNotification false")
                        operator.operateFailed()
                        return
                    }
                    val descriptor = if (useCharacterDescriptor) {
                        character.getDescriptor(character.uuid)
                    } else {
                        character.getDescriptor(
                            UUID.fromString(
                                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
                            )
                        )
                    }
                    if (descriptor == null) {
                        LogUtil.d(TAG, "Gatt characteristic's descriptor is null")
                        operator.operateFailed()
                        return
                    }
                    descriptor.value = if (enable) {
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }
                    val flag2 = bluetoothGatt?.writeDescriptor(descriptor)
                    if (flag2 != true) {
                        LogUtil.d(TAG, "Gatt writeDescriptor false")
                        operator.operateFailed()
                        return
                    }
                } else {
                    LogUtil.d(TAG, "Gatt this characteristic not support notify")
                    operator.operateFailed()
                    return
                }
            }
        } else {
            operator.operateFailed()
        }
    }

    override fun readRemoteRssi() {
        if (checkService()) {
            val flag = bluetoothGatt?.readRemoteRssi()
            if (flag != true) {
                LogUtil.d(TAG, "Gatt readRemoteRssi failed")
            }
        } else {
            rssiOperator?.operateFailed()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun setMtu(mtu: Int) {
        if (checkService()) {
            val flag = bluetoothGatt?.requestMtu(mtu)
            if (flag != true) {
                LogUtil.d(TAG, "Gatt setMtu failed")
            }
        } else {
            mtuOperator?.operateFailed()
        }
    }

    private fun checkService(): Boolean {
        if (bluetoothGatt == null) {
            LogUtil.d(TAG, "Gatt bluetoothGatt is null")
            return false
        }
        if (!isServicesDiscovered) {
            LogUtil.d(TAG, "Gatt service is not discovered")
            return false
        }
        if (lastState != BluetoothProfile.STATE_CONNECTED) {
            LogUtil.d(TAG, "Gatt service is not connected")
            return false
        }
        return true
    }

    @Synchronized
    private fun disconnectGatt() {
        bluetoothGatt?.disconnect()
    }

    @Synchronized
    private fun refreshDeviceCache() {
        try {
            val refresh = BluetoothGatt::class.java.getMethod("refresh")
            if (bluetoothGatt != null) {
                val success = refresh.invoke(bluetoothGatt) as Boolean
                LogUtil.d(TAG, "refreshDeviceCache, is success:  $success")
            }
        } catch (e: Exception) {
            LogUtil.d(TAG, "exception occur while refreshing device: " + e.message)
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun closeBluetoothGatt() {
        bluetoothGatt?.close()
    }
}
