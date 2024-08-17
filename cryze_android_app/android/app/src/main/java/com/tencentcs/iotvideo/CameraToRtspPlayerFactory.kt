package com.tencentcs.iotvideo

import com.tencentcs.iotvideo.utils.LogUtils

class CameraToRtspPlayerFactory(val cameraId: String, val context: MainActivity) {

    private var rtspPlayer: CameraToRtspPlayer? = null
    private var runnerThread: Thread? = null

    fun register()
    {
        createPlayer()
        startPlayer()
    }

    private fun createPlayer(): Unit
    {
        rtspPlayer = CameraToRtspPlayer(cameraId, object : IRtspPlayerEventHandler
        {
            override fun onWatchdogTimeout() {
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
            context.viewModel.addCamera(rtspPlayer!!)
        }

        rtspPlayer?.start()
    }

    private fun stopPlayer()
    {
        rtspPlayer?.stop()
        rtspPlayer?.release()
        rtspPlayer = null

        runnerThread?.interrupt()
        runnerThread = null

        try {
            context.runOnUiThread {
                context.viewModel.removeCamera(cameraId)
            }
        } catch (e: Exception) {
            LogUtils.e(CameraToRtspPlayerFactory::class.java.simpleName, "Exception in stopPlayer while removing camera from viewModel ${e.message}")
        }
    }

    fun release()
    {
        stopPlayer()
    }

}