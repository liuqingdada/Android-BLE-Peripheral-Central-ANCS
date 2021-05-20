package com.android.common.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.*
import java.util.concurrent.*

/**
 * Created by cooper
 * 20-6-1.
 * Email: 1239604859@qq.com
 */
class SerialExecutor : Executor {
    private val tasks = ArrayDeque<Runnable>()
    private var active: Runnable? = null

    @Synchronized
    override fun execute(r: Runnable) {
        tasks.offer(Runnable {
            try {
                r.run()
            } finally {
                scheduleNext()
            }
        })
        if (active == null) {
            scheduleNext()
        }
    }

    private fun scheduleNext() {
        active = tasks.poll()
        active?.run {
            threadPoolExecutor.execute(this)
        }
    }

    companion object {
        private const val CORE_POOL_SIZE = 1
        private const val MAXIMUM_POOL_SIZE = 20
        private const val BACKUP_POOL_SIZE = 5
        private const val KEEP_ALIVE_SECONDS: Long = 3

        val workThread = HandlerThread("SerialExecutor-Work")

        // Used only for rejected executions.
        // Initialization protected by sRunOnSerialPolicy lock.
        private var backupExecutor: ThreadPoolExecutor? = null
        private var backupExecutorQueue: LinkedBlockingQueue<Runnable>? = null

        private var threadPoolExecutor: ThreadPoolExecutor = ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            SynchronousQueue(),
            SerialPolicy()
        )

        init {
            workThread.start()
        }

        val MAIN_HANDLER = Handler(Looper.getMainLooper())
        val SERIAL_EXECUTOR = SerialExecutor()
        val THREAD_POOL_EXECUTOR = threadPoolExecutor
        val WORK_HANDLER = Handler(workThread.looper)
    }

    class SerialPolicy : RejectedExecutionHandler {
        override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
            // As a last ditch fallback, run it on an executor with an unbounded queue.
            // Create this executor lazily, hopefully almost never.
            synchronized(this) {
                if (backupExecutor == null) {
                    backupExecutorQueue = LinkedBlockingQueue()
                    backupExecutor = ThreadPoolExecutor(
                        BACKUP_POOL_SIZE,
                        BACKUP_POOL_SIZE,
                        KEEP_ALIVE_SECONDS,
                        TimeUnit.SECONDS,
                        backupExecutorQueue
                    )
                    backupExecutor?.allowCoreThreadTimeOut(true)
                }
            }
            backupExecutor?.execute(r)
        }
    }
}

fun serialExecute(r: Runnable) {
    SerialExecutor.SERIAL_EXECUTOR.execute(r)
}

fun mainThread(delayMillis: Long = 0, r: Runnable) {
    SerialExecutor.MAIN_HANDLER.postDelayed(r, delayMillis)
}

fun workThread(delayMillis: Long = 0, r: Runnable) {
    SerialExecutor.WORK_HANDLER.postDelayed(r, delayMillis)
}

fun execute(r: Runnable) {
    SerialExecutor.THREAD_POOL_EXECUTOR.execute(r)
}