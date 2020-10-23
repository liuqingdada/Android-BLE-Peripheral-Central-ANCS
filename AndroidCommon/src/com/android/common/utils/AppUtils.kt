package com.android.common.utils

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.text.TextUtils

/**
 * Created by cooper
 * 20-10-19.
 * Email: 1239604859@qq.com
 */
object AppUtils {
    private const val TAG = "AppUtils"

    lateinit var application: Application

    val handler = Handler(Looper.getMainLooper())

    fun init(application: Application) {
        AppUtils.application = application
    }
}