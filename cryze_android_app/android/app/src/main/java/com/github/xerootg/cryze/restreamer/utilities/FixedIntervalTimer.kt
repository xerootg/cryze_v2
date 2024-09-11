package com.github.xerootg.cryze.restreamer.utilities

import com.tencentcs.iotvideo.utils.LogUtils
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FixedIntervalTimer(private var interval: Duration, private val task: Runnable) {
    private var scheduler: ScheduledExecutorService? = null
    private var isRunning = false

    fun start() {
        if (!isRunning) {
            scheduler = Executors.newSingleThreadScheduledExecutor()
            scheduler?.scheduleWithFixedDelay(task, interval.inWholeMilliseconds, interval.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            isRunning = true
            LogUtils.i(FixedIntervalTimer::class.simpleName, "FixedIntervalTimer start, interval: $interval")
        }
    }

    fun stop() {
        if (isRunning) {
            scheduler?.shutdown()
            scheduler?.awaitTermination(5.seconds.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            scheduler = null
            isRunning = false
            LogUtils.i(FixedIntervalTimer::class.simpleName, "FixedIntervalTimer stop")
        }
    }

    fun resetTimer() {
        if (isRunning) {
            LogUtils.i(FixedIntervalTimer::class.simpleName, "FixedIntervalTimer resetTimer")
            stop()
            start()
        }
    }

    fun isRunning(): Boolean {
        return isRunning
    }

    fun changeInterval(interval: Duration) {
        this.interval = interval
        resetTimer()
    }
}