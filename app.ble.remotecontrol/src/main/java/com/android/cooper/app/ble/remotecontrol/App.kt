package com.android.cooper.app.ble.remotecontrol

import android.app.Application
import android.content.Context
import com.android.cooper.app.ble.remotecontrol.log.LogTreeProxy
import com.android.common.utils.AppUtils
import me.weishu.reflection.Reflection

/**
 * Created by cooper
 * 20-10-19.
 * Email: 1239604859@qq.com
 */
class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base);
    }

    override fun onCreate() {
        super.onCreate()
        AppUtils.init(this)

        LogTreeProxy.main()
    }
}