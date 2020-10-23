package com.suhen.android.libble.utils

import android.app.Application
import android.os.Handler
import android.os.Looper

/**
 * Created by cooper
 * 20-10-19.
 * Email: 1239604859@qq.com
 */
object AppUtils {
    lateinit var application: Application

    val handler = Handler(Looper.getMainLooper())

    fun init(application: Application) {
        AppUtils.application = application
    }
}