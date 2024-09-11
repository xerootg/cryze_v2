package com.github.xerootg.cryze.restreamer.rtspmuxer

import com.pedro.common.ConnectChecker
import com.pedro.rtsp.rtsp.RtspClient
import com.github.xerootg.cryze.restreamer.interfaces.IRendererCallback
import com.github.xerootg.cryze.restreamer.rtspmuxer.sources.RemuxerVideoSource
import com.github.xerootg.cryze.restreamer.rtspmuxer.sources.RemuxerAudioSource
import com.tencentcs.iotvideo.utils.LogUtils
import java.util.concurrent.atomic.AtomicBoolean
import com.github.xerootg.cryze.BuildConfig.CRYZE_RTSP_SERVER
import com.github.xerootg.cryze.httpclient.responses.CameraInfo

interface ISourceReadyCallback {
    fun sourceReady()
}

class RtspRemuxer(cameraInfo: CameraInfo, cb: IRendererCallback) : ConnectChecker {
    private val rtspPort = 8554
    private val rtspSubpath = "live"
    // the server url is the rtsp server url with the port and the stream name
    // the stream name is "live/" the device id, or the stream name if it exists
    private val serverUrl = "rtsp://$CRYZE_RTSP_SERVER:$rtspPort/${cameraInfo.streamName?: "$rtspSubpath/${cameraInfo.cameraId}"}"
    private var muxer: RtspClient = RtspClient(this)

    private var initInterrupted = AtomicBoolean(false)
    private var isConnecting = AtomicBoolean(false)

    fun startMuxer(){
        if(
            !isConnecting.get()&& // if there's no connection attempt in progress
            !muxer.isStreaming && // if the muxer isn't already streaming
            videoDecoder.initialized && // if the video source has been initialized
            audioDecoder.initialized) // if the audio source has been initialized
        {
            isConnecting.set(true)
            muxer.connect(serverUrl)
            muxer.setLogs(false)
        }
    }

    fun stopMuxer(){
        // the video source will continually attempt to reconnect if we don't stop it
        videoDecoder.doNotReconnect.set(true)

        // disconnects the rtsp stream, does NOT stop the packet processing.
        muxer.disconnect()
        muxer.setLogs(true)
    }

    // after both codecs have had init called, we can start the muxer
    // I'm letting startMuxer guard against starting twice and init
    private val readyCallback = object : ISourceReadyCallback {
        override fun sourceReady() {
            LogUtils.i(TAG, "Source ready, starting muxer")
            startMuxer()
        }
    }

    // the callback is used to signal up to the lifecycle that we are not stalled.
    val audioDecoder: RemuxerAudioSource = RemuxerAudioSource(muxer, readyCallback, cb)

    var videoDecoder: RemuxerVideoSource = RemuxerVideoSource(muxer, readyCallback)

    fun release(){
        LogUtils.d(TAG, "Releasing resources")

        // prevents the start() method from being called again
        initInterrupted.set(true)

        // shuts down the rtsp stream
        stopMuxer()

        // stops the audio and video streams from processing packets
        audioDecoder.stopStream()
        videoDecoder.stopStream()

        // releases the audio and video sources
        audioDecoder.release()
        videoDecoder.release()

        LogUtils.d(TAG, "Released resources")
    }

    override fun onConnectionStarted(url: String) {
        LogUtils.i(TAG, "Connection started: $url")

        // it's not entirely obvious, but this starts the packet processing
        // until this is called, the video source will wait for and collect
        // sps and pps NALs, which are required for the rtsp stream to start
        audioDecoder.startStream()
        videoDecoder.startStream()
    }

    override fun onConnectionSuccess() {
        isConnecting.set(false) // the connection is no longer in progress, subsequent calls to startMuxer will be allowed
        LogUtils.i(TAG, "Connection success")

    }

    override fun onConnectionFailed(reason: String) {
        LogUtils.e(TAG, "Connection failed: $reason")
        Thread {
            isConnecting.set(false) // the connection is no longer in progress, subsequent calls to startMuxer will be allowed
            Thread.sleep(5_000) // wait 5 seconds before trying again
            muxer.connect(serverUrl, true) // try to connect again, this time with a reconnect flag
        }.start()
    }

    override fun onDisconnect() {
        LogUtils.i(TAG, "Disconnected")
        audioDecoder.stopStream() // stops the audio stream from processing packets
        videoDecoder.stopStream() // stops the video stream from processing packets
    }

    override fun onAuthError() {
        LogUtils.e(TAG, "Auth error")
    }

    override fun onAuthSuccess() {
        LogUtils.i(TAG, "Auth success")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("H264RtspRemuxer{").append("\n\t\t\t")
        sb.append("audioDecoder=").append(audioDecoder).append("\n\t\t\t")
        sb.append("videoDecoder=").append(videoDecoder).append("\n\t\t\t")
        sb.append("muxerIsStreaming=").append(muxer.isStreaming).append("\n\t\t\t")
        sb.append("muxer=").append(muxer).append("\n\t\t\t")
        sb.append('}')
        return sb.toString()
    }

    companion object {
        private val TAG = RtspRemuxer::class.simpleName
    }
}

