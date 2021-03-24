package com.android.lib.ble.message

import com.android.common.utils.asyncExecute
import java.util.*

/**
 * Created by cooper
 * 20-6-1.
 * Email: 1239604859@qq.com
 */
class ActiveSerialExecutor {
    private val tasks = ArrayDeque<Runnable>()

    @Volatile
    private var isClean = false

    var active: Runnable? = null

    @Synchronized
    fun execute(r: ActiveRunnable) {
        tasks.offer {
            try {
                r.run()
                while (r.isIdle() || isClean) {
                    break
                }
            } finally {
                scheduleNext()
            }
        }
        if (active == null) {
            scheduleNext()
        }
    }

    private fun scheduleNext() {
        active = tasks.poll()
        active?.let {
            asyncExecute(it)
        }
    }

    @Synchronized
    fun ready() {
        isClean = false
    }

    @Synchronized
    fun clear() {
        isClean = true
        tasks.clear()
    }
}

abstract class ActiveRunnable : Runnable {
    abstract fun isIdle(): Boolean
}