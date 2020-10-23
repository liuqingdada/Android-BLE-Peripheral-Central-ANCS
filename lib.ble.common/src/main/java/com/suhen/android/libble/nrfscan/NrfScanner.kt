package com.suhen.android.libble.nrfscan

import android.os.Build
import com.android.common.utils.LogUtil
import com.android.common.utils.AppUtils
import com.android.common.utils.SharedPrefsUtils
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanSettings

/**
 * Created by andy
 * 2019-07-26.
 * Email: 1239604859@qq.com
 */
internal class NrfScanner(private val scanCallback: ScanCallback) {
    companion object {
        private const val TAG = "NrfScanner"

        private const val REPORT_DELAY: Long = FastPairConstant.SOURCE.BLE_SCAN_REPORT_DELAY
    }

    private val context = AppUtils.application

    fun startLeCompatScan() {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(REPORT_DELAY)
            .setUseHardwareBatchingIfSupported(false)
            .build()

        val scanner = BluetoothLeScannerCompat.getScanner()

        val filters = arrayListOf(ScanFilter.Builder().build())

        scanner.startScan(filters, settings, scanCallback)
        SharedPrefsUtils.putBoolean(
            context,
            FastPairConstant.Extra.SP_NAME,
            FastPairConstant.Extra.NRF_SCAN_STATUS,
            true
        )
        LogUtil.d(TAG, "startLeCompatScan: ")
    }

    fun stopLeCompatScan() {
        val scanner = BluetoothLeScannerCompat.getScanner()
        scanner.stopScan(scanCallback)
        SharedPrefsUtils.putBoolean(
            context,
            FastPairConstant.Extra.SP_NAME,
            FastPairConstant.Extra.NRF_SCAN_STATUS,
            false
        )
        LogUtil.d(TAG, "stopLeCompatScan: ")
    }

    fun getNativeCallback(scanCallback: ScanCallback): android.bluetooth.le.ScanCallback? {
        val scanner = BluetoothLeScannerCompat.getScanner()
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> getRealCallback(
                scanner,
                scanner.javaClass.superclass?.superclass,
                scanCallback
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> getRealCallback(
                scanner,
                scanner.javaClass.superclass,
                scanCallback
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> getRealCallback(
                scanner,
                scanner.javaClass,
                scanCallback
            )
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getRealCallback(
        scanner: BluetoothLeScannerCompat,
        clazz: Class<*>?,
        scanCallback: ScanCallback
    ): android.bluetooth.le.ScanCallback? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val feildWrappers = clazz?.getDeclaredField("wrappers")
                feildWrappers?.isAccessible = true
                val map = feildWrappers?.get(scanner)
                if (map != null) {
                    val wrappers = map as Map<ScanCallback, *>
                    val wrapper = wrappers[scanCallback]
                    val fieldCallback = wrapper?.javaClass?.getDeclaredField("nativeCallback")
                    fieldCallback?.isAccessible = true
                    val nativeCallback = fieldCallback?.get(wrapper)
                    return nativeCallback as? android.bluetooth.le.ScanCallback
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, "getNativeCallback: ", e)
            }
        }
        return null
    }
}