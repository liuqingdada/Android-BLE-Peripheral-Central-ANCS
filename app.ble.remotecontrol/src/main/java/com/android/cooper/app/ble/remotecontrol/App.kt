package com.android.cooper.app.ble.remotecontrol

import android.app.Application
import android.content.Context
import com.android.common.utils.ApplicationUtils
import com.android.cooper.app.ble.remotecontrol.log.LogTreeProxy
import com.suhen.android.libble.utils.AppUtils
import me.weishu.reflection.Reflection

/**
 * Created by cooper
 * 20-10-19.
 * Email: 1239604859@qq.com
 */
class App : Application() {
    companion object {
        private const val BLE_CONNECT_TIMEOUT = 60 * 1000.toLong()
        private const val BLE_RECONNECT_INTERVAL: Long = 100
        private const val BLE_RECONNECT_COUNT = 0
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base);
    }

    override fun onCreate() {
        super.onCreate()
        ApplicationUtils.init(this)
        AppUtils.init(this)

        LogTreeProxy.main()
    }
}