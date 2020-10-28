package com.android.cooper.app.ble.remotecontrol.main

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.common.utils.AppUtils
import com.android.common.utils.LogUtil
import com.android.common.utils.SharedPrefsUtils
import com.android.cooper.app.ble.remotecontrol.R
import com.android.cooper.lib.blelogic.message.KeyEventMessage
import com.suhen.android.libble.BLE
import com.suhen.android.libble.central.CentralGattCallback
import com.suhen.android.libble.central.CentralGattOperator
import com.suhen.android.libble.central.ICentral
import com.suhen.android.libble.nrfscan.BleCompatibility
import com.suhen.android.libble.nrfscan.BleScanManager
import com.suhen.android.libble.nrfscan.FastPairConstant
import com.suhen.android.libble.permission.PermissionWizard
import com.suhen.android.libble.utils.WeakHandler
import kotlinx.android.synthetic.main.activity_main.*
import no.nordicsemi.android.support.v18.scanner.ScanResult

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"

        // UUID
        private const val SERVICE_UUID = "4622c045-1cd2-4211-adc5-89df72c789ec"

        private const val CHAR_INDICATE_UUID = "4622c046-1cd2-4211-adc5-89df72c789ec"

        private const val CHAR_WRITE_UUID = "4622c047-1cd2-4211-adc5-89df72c789ec"

        private const val LE_SCAN_DELAY: Long = 1000
        private const val GAIA_SCAN_DELAY = 5 * 1000.toLong()
        private const val LE_STOP_SCAN_DELAY = 6 * 1000.toLong()
        private const val LE_MAX_SCAN_TIME = 20 * 60 * 1000.toLong()

        /**
         * {@see android.bluetooth.le.BluetoothLeScanner.BleScanCallbackWrapper}
         * 我们需要更长一点的时间
         */
        private const val REGISTRATION_CALLBACK_TIMEOUT_MILLIS = 3 * 1000.toLong()
    }

    @Volatile
    private var isScreenOn = true

    @Volatile
    private var isReportScan = false

    private val uiHandler = UiHandler(this)

    private class UiHandler(owner: MainActivity) : WeakHandler<MainActivity>(owner) {
        companion object {
            private const val MSG_START_BLE_SCAN = 0xFFFF
            private const val MSG_STOP_BLE_SCAN = 0xFFFE
            private const val MSG_GET_SCAN_CLIENT_ID = 0xFFFD
            private const val MSG_RESCAN_FOR_GET_SCANID = 0xFFFC
            const val MSG_BLE_SCAN_MAX_TIME = 0xFFFB
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val owner = owner ?: return
            when (msg.what) {
                MSG_START_BLE_SCAN -> {
                    if (BleScanManager.adapter.isEnabled) {
                        startBleScanLogic()
                    }
                }
                MSG_STOP_BLE_SCAN -> {
                    if (BleScanManager.adapter.isEnabled) {
                        if (!owner.needRescan()) {
                            owner.stopLeScanInternal()
                        }
                    }
                }
                MSG_GET_SCAN_CLIENT_ID -> {
                    if (BleScanManager.adapter.isEnabled) {
                        owner.saveScannerId()
                    }
                }
                MSG_RESCAN_FOR_GET_SCANID -> {
                    msgBleRescan()
                }
                MSG_BLE_SCAN_MAX_TIME -> {
                    msgBleRescan()
                }
            }
        }

        private fun msgBleRescan() {
            owner.isReportScan = false
            owner.stopLeScanInternal()
            owner.startLeScan()
        }

        private fun startBleScanLogic() {
            LogUtil.d(TAG, "startBleScanLogic: ")
            if (!BLE.isSupportBle(AppUtils.application)) {
                return
            }
            if (owner.needRescan()) {
                owner.startLeScanInternal()
            }
        }

        /**
         * Start Le Scan
         */
        fun dispatchStartBleMsg() {
            removeMessages(MSG_STOP_BLE_SCAN)
            removeMessages(MSG_START_BLE_SCAN)
            sendEmptyMessageDelayed(MSG_START_BLE_SCAN, LE_SCAN_DELAY)
        }

        /**
         * Stop Le Scan
         * 安卓7.0不允许在30s内连续扫描5次，否则无法扫描到任何设备，只能重启app
         *
         *
         * 保险起见，停止扫描的时间设置为6s
         */
        fun dispatchStopBleScan() {
            removeMessages(MSG_START_BLE_SCAN)
            removeMessages(MSG_STOP_BLE_SCAN)
            sendEmptyMessageDelayed(MSG_STOP_BLE_SCAN, LE_STOP_SCAN_DELAY)
        }

        fun dispatchGetScannerId() {
            removeMessages(MSG_GET_SCAN_CLIENT_ID)
            sendEmptyMessageDelayed(MSG_GET_SCAN_CLIENT_ID, REGISTRATION_CALLBACK_TIMEOUT_MILLIS)
        }

        /**
         * Just rescan
         */
        fun dispatchRegetScannerId() {
            removeMessages(MSG_RESCAN_FOR_GET_SCANID)
            sendEmptyMessageDelayed(MSG_RESCAN_FOR_GET_SCANID, LE_STOP_SCAN_DELAY)
        }

        /**
         * Restart Le Scan
         * 安卓7.0不允许应用连续扫描超过30分钟
         */
        fun dispatchBleScanMaxTime() {
            removeMessages(MSG_BLE_SCAN_MAX_TIME)
            sendEmptyMessageDelayed(MSG_BLE_SCAN_MAX_TIME, LE_MAX_SCAN_TIME)
        }
    }

    private val mSystemStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_ON == intent.action) {
                isScreenOn = true
                startLeScan()
                LogUtil.i(TAG, "onReceive: ACTION_SCREEN_ON")
            }
            if (Intent.ACTION_SCREEN_OFF == intent.action) {
                isScreenOn = false
                stopLeScan()
                LogUtil.i(TAG, "onReceive: ACTION_SCREEN_OFF")
            }
            if (Intent.ACTION_USER_PRESENT == intent.action) {
                LogUtil.i(TAG, "onReceive: ACTION_USER_PRESENT")
            }
            if (LocationManager.MODE_CHANGED_ACTION == intent.action) {
                if (PermissionWizard.isLocationServiceAllowed(context)) {
                    startLeScan()
                }
            }
        }
    }

    private val bleScanCallback = object : BleScanManager.BleScanCallback {
        override fun onBluetoothOpen() {
            startLeScan()
        }

        override fun onBluetoothClose() {
            stopLeScanInternal()
        }

        override fun onRemoteDeviceFound(result: ScanResult) {
        }

        override fun onScanFailed(code: Int) {
            LogUtil.e(TAG, "BLE SCAN onScanFailed: $code")
            if (code == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                BleCompatibility.releaseAllScanClient()
                restartScan()
            }
        }

        override fun onReported(results: List<ScanResult>) {
            if (!isReportScan) {
                return
            }
            results.forEach {
                LogUtil.d(TAG, "onReported: ${it.device.name}")
                if (it.device.name == "Suhen") {
                    stopLeScan()

                    uiHandler.post {
                        btConnect.tag = it
                        btConnect.isEnabled = true
                    }
                }
            }
        }

        override fun onStartScan() {
            uiHandler.dispatchGetScannerId()
            uiHandler.dispatchBleScanMaxTime()
        }

        override fun onStopScan() {
            uiHandler.removeMessages(UiHandler.MSG_BLE_SCAN_MAX_TIME)
        }
    }

    private var central: ICentral? = null

    fun needRescan(): Boolean {
        // 亮屏
        LogUtil.d(TAG, "screenOn: $isScreenOn")
        return isScreenOn
    }

    fun startLeScan() {
        isReportScan = true
        uiHandler.dispatchStartBleMsg()
    }

    /**
     * Only call this in ticpod pair activity
     */
    fun forceStartLeScan() {
        isReportScan = true
        startLeScanInternal()
    }

    /**
     * Only call this when user can't scaned device
     */
    fun restartScan() {
        uiHandler.dispatchRegetScannerId()
        BleCompatibility.refreshBleAppFromSystem(this, packageName)
    }

    /**
     * delays the reconnection about 5s for ADK6 implementation
     */
    fun startLeScanDelay() {
        uiHandler.postDelayed({
            startLeScan()
        }, GAIA_SCAN_DELAY)
    }

    fun stopLeScan() {
        isReportScan = false
        uiHandler.dispatchStopBleScan()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BleScanManager.bleScanCallback = bleScanCallback

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        filter.addAction(LocationManager.MODE_CHANGED_ACTION)
        registerReceiver(mSystemStateReceiver, filter)

        clearScannerId()

        btScan.setOnClickListener {
            if (!BleScanManager.adapter.isEnabled) {
                Toast.makeText(this, "请打开蓝牙", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!PermissionWizard.isLocationServiceAllowed(this)) {
                Toast.makeText(this, "请打开位置信息/GPS开关", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val bleScanPermission = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            ActivityCompat.requestPermissions(this, bleScanPermission, 100)
        }

        btHome.setOnClickListener {
            val message = KeyEventMessage(KeyEvent.KEYCODE_HOME)
            central?.writeCharacter(centralGattOperator, message.subpackage(message.payload)[0])
        }
        btBack.setOnClickListener {
            val message = KeyEventMessage(KeyEvent.KEYCODE_BACK)
            central?.writeCharacter(centralGattOperator, message.subpackage(message.payload)[0])
        }
        btVolumeUp.setOnClickListener {
            val message = KeyEventMessage(KeyEvent.KEYCODE_VOLUME_UP)
            central?.writeCharacter(centralGattOperator, message.subpackage(message.payload)[0])
        }
        btVolumeDown.setOnClickListener {
            val message = KeyEventMessage(KeyEvent.KEYCODE_VOLUME_DOWN)
            central?.writeCharacter(centralGattOperator, message.subpackage(message.payload)[0])
        }

        btConnect.setOnClickListener {
            val scanResult = it.tag as? ScanResult
            if (scanResult == null) {
                Toast.makeText(this, "请重新扫描", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                connectBle(scanResult)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        btDisconnect.setOnClickListener {
            central?.disconnect()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LogUtil.d(
            TAG,
            "onRequestPermissionsResult: $requestCode, ${grantResults.contentToString()}"
        )
        if (requestCode == 100 && PermissionWizard.verifyPermissions(grantResults)) {
            startLeScan()
        }
    }

    private fun startLeScanInternal() {
        BleScanManager.startLeScan()
    }

    private fun stopLeScanInternal() {
        BleScanManager.stopLeScan()
    }

    private fun saveScannerId() {
        val nativeCallback: ScanCallback? = BleScanManager.getNativeCallback()
        if (nativeCallback != null) {
            val scannerId: Int = BleCompatibility.getScanClientId(nativeCallback)
            if (scannerId != -1) {
                SharedPrefsUtils.putInt(
                    this,
                    FastPairConstant.Extra.SP_NAME,
                    FastPairConstant.Extra.KEY_SCANNER_ID,
                    scannerId
                )
                LogUtil.i(TAG, "saveScannerId: $scannerId")
            }
        }
    }

    private fun clearScannerId() {
        val scanState: Boolean = SharedPrefsUtils.getBoolean(
            this,
            FastPairConstant.Extra.SP_NAME,
            FastPairConstant.Extra.NRF_SCAN_STATUS,
            false
        )
        if (!scanState) {
            LogUtil.d(TAG, "App last ble scan state is stop")
            return
        }
        val scannerId: Int = SharedPrefsUtils.getInt(
            this,
            FastPairConstant.Extra.SP_NAME,
            FastPairConstant.Extra.KEY_SCANNER_ID,
            -1
        )
        if (scannerId != -1) {
            SharedPrefsUtils.putInt(
                this,
                FastPairConstant.Extra.SP_NAME,
                FastPairConstant.Extra.KEY_SCANNER_ID,
                -1
            )
            BleCompatibility.releaseScanClient(scannerId)
            LogUtil.i(TAG, "clearScannerId: $scannerId")
        }
    }

    @Throws(Exception::class)
    private fun connectBle(result: ScanResult) {
        val device = result.device
        central?.device?.let {
            if (it == device) {
                central?.disconnect()
                central = null
            }
        }
        if (central == null) {
            central = BLE.newCentral(SimpleBleCentral::class.java, device)
            central?.centralGattCallback = centralGattCallback
            central?.addOperator(centralGattOperator)
        }
        if (central?.lastState == BluetoothProfile.STATE_DISCONNECTED) {
            central?.connect(false)
        }
    }

    private val centralGattCallback = object : CentralGattCallback {
        override fun onStartConnect(gatt: BluetoothGatt) {
        }

        override fun onConnectFailed(status: Int) {
        }

        override fun onConnected(gatt: BluetoothGatt, status: Int) {
            uiHandler.postDelayed({
                val flag = gatt.discoverServices()
                LogUtil.d(TAG, "discoverServices: $flag")
            }, 500)
        }

        override fun onDisconnected(gatt: BluetoothGatt, status: Int) {
            uiHandler.post {
                btHome.isEnabled = false
                btBack.isEnabled = false
                btVolumeUp.isEnabled = false
                btVolumeDown.isEnabled = false
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            for (service in gatt.services) {
                LogUtil.d(TAG, "${service.uuid}")
                for (characteristic in service.characteristics) {
                    LogUtil.d(
                        TAG, "\t" + "${characteristic.uuid}, " +
                                "${characteristic.properties}, " +
                                "${characteristic.permissions}"
                    )
                }
            }

            uiHandler.post {
                btHome.isEnabled = true
                btBack.isEnabled = true
                btVolumeUp.isEnabled = true
                btVolumeDown.isEnabled = true
            }
        }

        override fun onServicesDiscoverFailed(gatt: BluetoothGatt, status: Int) {
        }
    }

    private val centralGattOperator = object : CentralGattOperator(
        SERVICE_UUID,
        CHAR_WRITE_UUID
    ) {
        override fun onWrite(value: ByteArray, status: Int) {
            LogUtil.d(TAG, "$characterUUID write ok")
        }
    }
}