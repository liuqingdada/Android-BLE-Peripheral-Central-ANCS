package com.android.cooper.app.miottvdemo

import android.app.Application
import com.android.cooper.app.miottvdemo.log.LogTreeProxy
import com.android.common.utils.AppUtils

/**
 * Created by cooper
 * 20-10-19.
 * Email: 1239604859@qq.com
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppUtils.init(this)

        LogTreeProxy.main()
    }
}