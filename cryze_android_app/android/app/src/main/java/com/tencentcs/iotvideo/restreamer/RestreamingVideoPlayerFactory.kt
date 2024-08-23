package com.tencentcs.iotvideo.restreamer

import com.tencentcs.iotvideo.MainActivity
import com.tencentcs.iotvideo.restreamer.interfaces.IPlayerEventCallback
import com.tencentcs.iotvideo.utils.LogUtils
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RestreamingVideoPlayerFactory(val cameraId: String, val context: MainActivity) {

    private var restreamingVideoPlayer: RestreamingVideoPlayer? = null

    // returns the factory to allow chaining
    fun register() : RestreamingVideoPlayerFactory
    {
        createPlayer()
        startPlayer()
        return this
    }

    private fun createPlayer(): Unit
    {
        restreamingVideoPlayer = RestreamingVideoPlayer(cameraId, context)
    }

    private fun startPlayer() {
        // i duplicated this so the watchdog restarts can register us again after release
        createPlayer()

        context.runOnUiThread {
            context.viewModel.addCamera(restreamingVideoPlayer!!)
        }

        restreamingVideoPlayer?.start()
    }

    private val executor = Executors.newSingleThreadExecutor()
    private fun runWithTimeout(timeoutMs: Long, unit: ()->Unit)
    {
        val future = executor.submit {
            unit()
        }
        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS) // Timeout after 5 seconds
        } catch (_: Exception) {
        }
    }

    private fun stopPlayer()
    {
        if (restreamingVideoPlayer != null) {
            LogUtils.i(RestreamingVideoPlayerFactory::class.java.simpleName, "Stopping player")
            runWithTimeout(5_000L, { restreamingVideoPlayer?.stop() })
            runWithTimeout(5_000L, { restreamingVideoPlayer?.release() })
            LogUtils.i(RestreamingVideoPlayerFactory::class.java.simpleName, "Stopped player")
            restreamingVideoPlayer = null
        }

        LogUtils.i(RestreamingVideoPlayerFactory::class.java.simpleName, "Player is released, forcing GC")

        try {
            context.runOnUiThread {
                context.viewModel.removeCamera(cameraId)
            }
        } catch (e: Exception) {
            LogUtils.e(RestreamingVideoPlayerFactory::class.java.simpleName, "Exception in stopPlayer while removing camera from viewModel ${e.message}")
        }
    }

    fun release()
    {
        LogUtils.d(RestreamingVideoPlayerFactory::class.java.simpleName, "Releasing player")
        stopPlayer()
    }


    fun finalize()
    {
        LogUtils.d(RestreamingVideoPlayerFactory::class.java.simpleName, "Finalizing player")
        release()
    }


}