package com.suhen.android.libble.nrfscan

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import com.android.common.utils.AppUtils
import com.android.common.utils.LogUtil
import com.suhen.android.libble.permission.PermissionWizard
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author: cooper
 * Date: 20-10-21.
 * Email: yangliuqing@xiaomi.com
 */
object BleScanManager {
    private const val TAG = "BleScanManager"

    private val context: Context by lazy {
        AppUtils.application
    }

    private val isStopScan = AtomicBoolean(true)

    private val nrfScanner by lazy {
        NrfScanner(scanCallbackCompat)
    }

    private val scanThread by lazy {
        val thread = HandlerThread("ble_scan_looper_thread")
        thread.start()
        thread
    }

    private val scanHandler by lazy {
        Handler(scanThread.looper)
    }

    val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    var bleScanCallback: BleScanCallback? = null

    private val scanCallbackCompat = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            scanHandler.post {
                bleScanCallback?.onRemoteDeviceFound(result)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            scanHandler.post {
                bleScanCallback?.onReported(results)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            scanHandler.post {
                bleScanCallback?.onScanFailed(errorCode)
            }
        }
    }

    fun startLeScan() {
        val isLocationServiceAllowed: Boolean = PermissionWizard.isLocationServiceAllowed(context)
        if (!isLocationServiceAllowed) {
            LogUtil.d(TAG, "startLeScan: LocationService is not allowed")
            return
        }
        if (!PermissionWizard.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        ) {
            LogUtil.d(TAG, "startLeScan: Location permission is not allowed")
            return
        }
        if (!isStopScan.get()) {
            LogUtil.d(TAG, "startLeScan: already in scan")
            return
        }
        LogUtil.d(TAG, "startLeScan: ")
        // Operate the nrf scanner and set the scan flag
        if (adapter.isEnabled) {
            if (isStopScan.compareAndSet(true, false)) {
                nrfScanner.startLeCompatScan()
                bleScanCallback?.onStartScan()
            }
        }
    }

    fun stopLeScan() {
        if (isStopScan.get()) {
            return
        }
        LogUtil.d(TAG, "stopLeScan: ")
        // Operate the nrf scanner and set the scan flag
        if (isStopScan.compareAndSet(false, true)) {
            nrfScanner.stopLeCompatScan()
            bleScanCallback?.onStopScan()
        }
    }

    fun getNativeCallback(): android.bluetooth.le.ScanCallback? {
        return nrfScanner.getNativeCallback(scanCallbackCompat)
    }

    private val bluetoothStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                val previousState = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                    BluetoothAdapter.STATE_OFF
                )
                when (state) {
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                        if (previousState != BluetoothAdapter.STATE_TURNING_OFF &&
                            previousState != BluetoothAdapter.STATE_OFF
                        ) {
                            LogUtil.e(TAG, "bluetooth OFF")
                            bleScanCallback?.onBluetoothClose()
                        }
                    }
                    BluetoothAdapter.STATE_ON -> {
                        LogUtil.e(TAG, "bluetooth ON")
                        bleScanCallback?.onBluetoothOpen()
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        LogUtil.e(TAG, "bluetooth TURNING_ON")
                    }
                }
            }
        }
    }

    interface BleScanCallback {
        fun onBluetoothOpen()

        fun onBluetoothClose()

        fun onRemoteDeviceFound(result: ScanResult)

        fun onScanFailed(code: Int)

        fun onReported(results: List<ScanResult>)

        fun onStartScan()

        fun onStopScan()
    }

    init {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStatusReceiver, intentFilter)
    }
}