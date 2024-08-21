package com.tencentcs.iotvideo.h264streamer

import com.tencentcs.iotvideo.MainActivity
import com.tencentcs.iotvideo.bitmapstream.CameraToH264StreamPlayer
import com.tencentcs.iotvideo.utils.LogUtils

class CameraToH264StreamPlayerFactory(val cameraId: String, val context: MainActivity) {

    private var h264StreamPlayer: CameraToH264StreamPlayer? = null
    private var runnerThread: Thread? = null

    fun register()
    {
        createPlayer()
        startPlayer()
    }

    private fun createPlayer(): Unit
    {
        h264StreamPlayer = CameraToH264StreamPlayer(cameraId, object :
            IH264PlayerEventHandler
        {
            override fun onWatchdogTimeout() {
                LogUtils.d(CameraToH264StreamPlayerFactory::class.java.simpleName, "$cameraId onWatchdogTimeout")
                stopPlayer() // kill this one before starting a new one

                runnerThread = Thread {
                    startPlayer()
                    // return the thread so we can start the new one fresh
                }
                runnerThread?.start() // let this thread return so we can start the new one fresh
            }

        }, context)
    }

    private fun startPlayer() {
        createPlayer()

        context.runOnUiThread {
            context.viewModel.addCamera(h264StreamPlayer!!)
        }

        h264StreamPlayer?.start()
    }

    private fun stopPlayer()
    {
        h264StreamPlayer?.stop()
        h264StreamPlayer?.release()
        h264StreamPlayer = null

        runnerThread?.interrupt()
        runnerThread?.join()
        runnerThread = null

        try {
            context.runOnUiThread {
                context.viewModel.removeCamera(cameraId)
            }
        } catch (e: Exception) {
            LogUtils.e(CameraToH264StreamPlayerFactory::class.java.simpleName, "Exception in stopPlayer while removing camera from viewModel ${e.message}")
        }
    }

    fun release()
    {
        LogUtils.d(CameraToH264StreamPlayerFactory::class.java.simpleName, "Releasing player")
        stopPlayer()
    }

}