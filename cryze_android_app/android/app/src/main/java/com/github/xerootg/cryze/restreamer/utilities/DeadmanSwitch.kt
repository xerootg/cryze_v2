package com.github.xerootg.cryze.restreamer.utilities

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

/**
 * DeadmanSwitch is a utility class that manages a timer to ensure that a certain action
 * (callback) is executed if a specified duration passes without the switch being activated.
 *
 * @param aliveFor The duration for which the switch remains activated.
 */
class DeadmanSwitch(private var aliveFor: Duration) {
    private val isActivated = AtomicBoolean(true)
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var primaryFuture: ScheduledFuture<*>? = null
    private var secondaryFuture: ScheduledFuture<*>? = null
    private var timeoutCallback: Runnable? = null
    private var callbackDuration: Duration? = null

    init {
        schedulePrimaryTimer()
    }

    /**
     * Schedules the primary timer for the specified `aliveFor` duration.
     * If the timer expires, it sets `isActivated` to false and schedules the secondary timer.
     */
    private var nextPrimaryExpirationTime = 0L
    @Synchronized
    private fun schedulePrimaryTimer() {
        primaryFuture?.cancel(false)
        primaryFuture = scheduler.schedule({
            isActivated.set(false)
            scheduleSecondaryTimer()
        }, aliveFor.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        nextPrimaryExpirationTime = System.currentTimeMillis() + aliveFor.inWholeMilliseconds
    }

    /**
     * Schedules the secondary timer for the specified `callbackDuration`.
     * If the timer expires, it runs the `timeoutCallback` if it is set.
     */
    @Synchronized
    private fun scheduleSecondaryTimer() {
        secondaryFuture?.cancel(false)
        secondaryFuture = scheduler.schedule({
            try {
                timeoutCallback?.run()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, callbackDuration?.inWholeMilliseconds ?: 0, TimeUnit.MILLISECONDS)
    }

    /**
     * Activates the switch, setting `isActivated` to true and rescheduling the primary timer.
     * This cancels any pending primary or secondary timers.
     */
    @Synchronized
    fun activate() {
        isActivated.set(true)
        primaryFuture?.cancel(false)
        secondaryFuture?.cancel(false)
        schedulePrimaryTimer()
    }

    /**
     * Returns the current activation state of the switch.
     */
    val activated: Boolean
        get() = isActivated.get()

    /**
     * Cancels any pending primary or secondary timers.
     */
    @Synchronized
    fun cancel() {
        primaryFuture?.cancel(false)
        secondaryFuture?.cancel(false)
        primaryFuture = null
        secondaryFuture = null
    }

    /**
     * Changes the interval for the primary timer and activates the switch.
     *
     * @param aliveForNow The new duration for which the switch remains activated.
     */
    @Synchronized
    fun changeInterval(aliveForNow: Duration) {
        aliveFor = aliveForNow
        // If the switch would expire after the new interval, don't change it
        if(aliveForNow.inWholeMilliseconds + System.currentTimeMillis() < nextPrimaryExpirationTime)
            return
        cancel()
        activate()
    }

    /**
     * Registers a timeout callback to be executed after the specified duration
     * if the switch is not activated.
     *
     * @param callback The callback to be executed.
     * @param duration The duration after which the callback is executed.
     */
    @Synchronized
    fun registerTimeoutCallback(callback: Runnable, duration: Duration) {
        timeoutCallback = callback
        callbackDuration = duration
    }

    /**
     * Shuts down the scheduler to release resources.
     */
    @Synchronized
    fun shutdown() {
        scheduler.shutdownNow()
    }
}