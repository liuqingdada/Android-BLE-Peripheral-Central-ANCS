package com.android.cooper.app.miottvdemo.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.android.common.utils.LogUtil
import com.android.cooper.app.miottvdemo.R
import com.android.cooper.app.miottvdemo.service.SimpleBlePeripheralImpl
import com.android.cooper.app.miottvdemo.service.SimpleBlePeripheralService
import com.android.cooper.lib.blelogic.message.KeyEventMessage
import com.android.cooper.lib.blelogic.message.Profile
import com.android.lib.ble.message.BleMessage
import com.android.lib.ble.message.BleMessageDecoder
import com.android.lib.ble.message.BleMessageListener
import com.android.lib.ble.peripheral.callback.BasePeripheralCallback
import com.android.lib.ble.peripheral.callback.BluetoothCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        fun nameForState(state: Int): String {
            return when (state) {
                BluetoothAdapter.STATE_OFF -> "OFF"
                BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
                BluetoothAdapter.STATE_ON -> "ON"
                BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
                else -> "?!?!? ($state)"
            }
        }
    }

    private var peripheralService: SimpleBlePeripheralService? = null
    private val handler = Handler(Looper.getMainLooper())
    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private var btOperate = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindPeriphera()
        /*btIndicate.setOnClickListener {
            peripheralService?.peripheral?.indicate(
                BlePeripheralImpl.SERVICE_UUID,
                BlePeripheralImpl.CHAR_INDICATE_UUID,
                EchoBleMessage()
            )
        }*/

        btOpenClose.setOnClickListener {
            if (btOperate) {
                if (adapter.state == BluetoothAdapter.STATE_ON) {
                    adapter.disable()
                }
                if (adapter.state == BluetoothAdapter.STATE_OFF) {
                    adapter.enable()
                }
                btOperate = false
            }
        }
        updateBtState()
    }

    private fun bindPeriphera() {
        val intent = Intent(this, SimpleBlePeripheralService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    @SuppressLint("SetTextI18n")
    private fun updateBtState() {
        btOpenClose.text = "Open/Close BT: ${nameForState(adapter.state)}"
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SimpleBlePeripheralService.PeripheralBinder
            peripheralService = binder.service

            peripheralService?.peripheral?.addBasePeripheralCallback(mBasePeripheralCallback)
            peripheralService?.peripheral?.setBluetoothCallback(object : BluetoothCallback {
                override fun onOpen() {
                    updateBtState()

                    btOperate = true
                }

                override fun onClose() {
                    updateBtState()

                    btOperate = true
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            peripheralService = null
        }
    }

    private class EchoBleMessage : BleMessage() {
        override fun payload(): ByteArray {
            return byteArrayOf(0x00, 0x01)
        }
    }

    private val mBasePeripheralCallback: BasePeripheralCallback = object : BasePeripheralCallback(
        SimpleBlePeripheralImpl.SERVICE_UUID,
        SimpleBlePeripheralImpl.CHAR_WRITE_UUID
    ) {
        val decoder = LimitedBleMessageDecoder { type, paylod ->
            LogUtil.d(TAG, "decoder: $type")
            when (type) {
                Profile.KEY_EVENT_MESSAGE_TYPE -> {
                    KeyEventMessage.build().parse(paylod)
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            val log = """
                onCharacteristicWriteRequest: device = $device
                characteristic = ${characteristic.uuid}
                preparedWrite = $preparedWrite
                responseNeeded = $responseNeeded
                offset = $offset
                value = ${value.contentToString()}
                """.trimIndent()
            LogUtil.d(TAG, log)
            handler.post {
                tvLog.append("$log \n")

                //nsvRoot.fullScroll(NestedScrollView.FOCUS_UP)
                nsvRoot.fullScroll(NestedScrollView.FOCUS_DOWN)
            }

            decoder.stash(value)
        }
    }

    private class LimitedBleMessageDecoder(
        listener: BleMessageListener,
    ) : BleMessageDecoder(listener) {
        companion object {
            private const val TAG = "BlePeripheralRCService"
            private const val MAX_MESSAGE_LENGTH = 1024 * 1024
        }

        override fun stash(data: ByteArray?) {
            if (cache.capacity() >= MAX_MESSAGE_LENGTH) {
                LogUtil.w(TAG, "Message length is over limit")
                cache.clear()
                cache = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
                return
            }
            super.stash(data)
        }
    }
}
