package com.android.lib.ble.message

import com.android.common.utils.execute
import java.util.*

/**
 * Created by cooper
 * 20-6-1.
 * Email: 1239604859@qq.com
 */
class ActiveSerialExecutor {
    private val tasks = ArrayDeque<RunnableWrapper>()

    var active: RunnableWrapper? = null
        private set

    @Synchronized
    fun execute(r: IndicateRunnable) {
        tasks.offer(object : RunnableWrapper(r) {
            override fun run() {
                try {
                    indicateRunnable.run()
                } finally {
                    scheduleNext()
                }
            }
        })
        if (active == null) {
            scheduleNext()
        }
    }

    private fun scheduleNext() {
        active = tasks.poll()
        active?.run {
            execute(this)
        }
    }

    @Synchronized
    fun quit() {
        tasks.clear()
    }

    abstract class RunnableWrapper(val indicateRunnable: IndicateRunnable) : Runnable
}
