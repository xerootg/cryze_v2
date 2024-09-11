package com.github.xerootg.cryze.restreamer

import com.github.xerootg.cryze.httpclient.CryzeHttpClient
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.IoTVideoSdk.PREFIX_THIRD_ID
import com.github.xerootg.cryze.httpclient.responses.CameraInfo
import com.tencentcs.iotvideo.iotvideoplayer.IErrorListener
import com.tencentcs.iotvideo.iotvideoplayer.IStatusListener
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer
import com.tencentcs.iotvideo.iotvideoplayer.PlayerState
import com.tencentcs.iotvideo.messagemgr.SubscribeError
import com.github.xerootg.cryze.restreamer.interfaces.IRendererCallback
import com.github.xerootg.cryze.restreamer.rtspmuxer.RtspRemuxer
import com.github.xerootg.cryze.restreamer.utilities.DeadmanSwitch
import com.github.xerootg.cryze.restreamer.utilities.FixedIntervalTimer
import com.tencent.mars.xlog.LogLevel
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.Utils.getErrorDescription
import com.tencentcs.iotvideo.utils.rxjava.ISubscribeStatusListener
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import android.os.Process
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

// the OnError callback is intentionally not implemented, as this
// class would not have the context to handle it
abstract class RestreamingLifecycleHandler(
    private val cameraInfo: CameraInfo
) : ISubscribeStatusListener<Boolean> {

    private val TAG = Companion.TAG + "::${cameraInfo.cameraId}"
    private var iotVideoPlayer: IoTVideoPlayer? = null

    // if this isn't poked regularly, we assume the decoder is stalled
    // Initially, 20s is a good interval, but we'll tighten it up after the first frame
    private val deadmanSwitch = DeadmanSwitch(20.seconds)
    var framesProcessed = 0L

    private val streamMuxer = RtspRemuxer(cameraInfo, object :
        IRendererCallback {
        override fun onFrame() {
            // if this is our first frame, then we need to tighten up the deadman switch interval
            // one frame per second is not viewable, but we are still processing frames
            if (framesProcessed == 0L) {
                deadmanSwitch.changeInterval(1.seconds)

                // if we don't get a frame in 30s, we'll restart the player
            }
            framesProcessed++
            deadmanSwitch.activate()
        }
    })

    val playbackState: PlayerState
        get() = iotVideoPlayer?.playState ?: PlayerState.STATE_UNKNOWN


    val isDecoderStalled: Boolean
        // isActivated is true when the switch is poked regularly
        get() = !deadmanSwitch.activated

    var isSubscribed = false
        private set // this is only set by the onSuccess callback

    private val tokenRenewalTimer = FixedIntervalTimer(1.hours) {
        LogUtils.i(TAG, "Renewing token")
        if (iotVideoPlayer?.isConnectedDevice == true) {
            iotVideoPlayer?.updateToken(CryzeHttpClient.getAccessCredentialByCameraId(cameraInfo.cameraId))
            deadmanSwitch.activate()
        }
    }

    fun stop() {
        streamMuxer.stopMuxer()
        var maxShutdownMs = 5000L

        iotVideoPlayer?.stop()
        while (iotVideoPlayer?.playState == PlayerState.STATE_PLAY && maxShutdownMs > 0) {
            maxShutdownMs -= 100; Thread.sleep(100)
        }
        tokenRenewalTimer.stop()
    }


    override fun onSuccess(success: Boolean) {
        tokenRenewalTimer.start()
        deadmanSwitch.activate()

        LogUtils.l(
            if (success) LogLevel.LEVEL_INFO else LogLevel.LEVEL_ERROR,
            TAG,
            "Subscribe: onSuccess: $success"
        )

        val playerErrorListener = object : IErrorListener {
            override fun onError(errorType: SubscribeError) {
                LogUtils.w(
                    TAG,
                    "errorListener onError for iotvideo player: $errorType with message: ${
                        getErrorDescription(
                            errorType.ordinal
                        )
                    }"
                )
            }
        }

        // listens for events from nativeDevState and nativeStatus
        val stateListener = object : IStatusListener {
            override fun onStatus(state: PlayerState) {
                LogUtils.i(TAG, "Player state: $state")
            }
        }

        iotVideoPlayer = IoTVideoPlayer(
            cameraInfo.cameraId,
            streamMuxer.videoDecoder,
            streamMuxer.audioDecoder,
            stateListener,
            playerErrorListener
        )

        LogUtils.i(TAG, "device isLanConnectable: ${isLanDevConnectable()}")

        iotVideoPlayer?.play() // start receiving packets
        // the first frame should be received within 5s
        deadmanSwitch.changeInterval(5.seconds)
        // if we don't get a frame in 30s, we'll restart the player
        deadmanSwitch.registerTimeoutCallback(deadmanTimeoutRoutine, 30.seconds)
        deadmanSwitch.activate() // prime the new interval
        isSubscribed = true // we're subscribed now, that was the hard part
    }

    // deadman timeouts should only run one at a time.
    private val deadmanLock: Lock = ReentrantLock()

    // I want a new task every time. a get should construct a new object every time
    private val deadmanTimeoutRoutine: Thread
        get() {
            return Thread {

                if (deadmanLock.tryLock()) {
                    try {
                        LogUtils.e(
                            TAG,
                            "deadman: No frames processed in 30s, current state: $playbackState: restarting player"
                        )

                        var maxShutdownMs = 5_000L

                        // magic does happen sometimes
                        if (!isDecoderStalled) {
                            LogUtils.i(TAG, "deadman: Player recovered without restart")
                            return@Thread
                        }

                        iotVideoPlayer?.stop()
                        while (
                            (playbackState != PlayerState.STATE_STOP &&
                                    playbackState != PlayerState.STATE_IDLE &&
                                    playbackState != PlayerState.STATE_UNKNOWN)
                            && maxShutdownMs > 0
                        ) {
                            // wait for the player to stop
                            maxShutdownMs -= 100; Thread.sleep(100)

                            // log every second
                            if (maxShutdownMs % 1000 == 0L) {
                                LogUtils.i(TAG, "deadman: Waiting for player to stop")
                            }

                            // log failure
                            if (maxShutdownMs <= 0) {
                                LogUtils.e(TAG, "deadman: Player did not stop")
                            }
                        }

                        // try refreshing the token
                        LogUtils.i(TAG, "deadman: Player state: $playbackState, refreshing token")
                        iotVideoPlayer?.updateToken(
                            CryzeHttpClient.getAccessCredentialByCameraId(
                                cameraInfo.cameraId
                            )
                        )

                        LogUtils.i(TAG, "deadman: Token refreshed, play state: $playbackState")
                        // At this point, one or two frames might flow, but ultimately, it's still stalled. sleep and confirm
                        Thread.sleep(20_000)

                        LogUtils.i(TAG, "deadman: Sleep to confirm recovery: play state: $playbackState")

                        // again, magic does happen sometimes. This SDK drives me nuts.
                        if (!isDecoderStalled) {
                            LogUtils.i(TAG, "deadman: Player recovered after token refresh")
                            return@Thread
                        }

                        LogUtils.i(TAG, "deadman: token refreshed, restarting player")
                        // restart the player and pray for the best
                        framesProcessed = 0L

                        // The max window for the player to come back to life is 60s
                        deadmanSwitch.changeInterval(60.seconds)
                        // lock in the new interval
                        deadmanSwitch.activate()
                        Thread {
                            iotVideoPlayer?.play()
                        }.start()
                        LogUtils.i(TAG, "deadman: Player restarted")

                        // just because the interval is set, doesnt mean the switch will expire.
                        // the 60s window still applies, this window only locks in if the onframe
                        // callback is called
                        deadmanSwitch.changeInterval(5.seconds)

                        // sleep off the rest of the 60s window
                        Thread.sleep(60_000)

                        // if we got only one frame, it's either been in the last
                        // 5s, or we're still stalled
                        if (!isDecoderStalled) {
                            LogUtils.i(TAG, "deadman: Player recovered")
                            return@Thread
                        } else {
                            LogUtils.e(TAG, "deadman: Player is stalled")

                            // kill the whole process. This is a last resort, but it's better than a
                            // dead stream until the next status change, which could not happen if we
                            // are "offline" which seems to happen at about 30 minutes from the first frame
                            // assumptions: the ensure_running.sh script will restart the activity
                            Process.killProcess(Process.myPid())
                        }
                    } finally {
                        // allow a new deadman routine to be scheduled
                        deadmanLock.unlock()
                    }
                } else {
                    LogUtils.e(TAG, "deadman: Timeout routine already running")
                    return@Thread
                }
            }
        }

    // This is only true when the container is in the same logical network as the camera
    // in docker, this means macvan and an assigned IP. this also means that this container probably
    // wont be accessible from the docker host, which makes development a bit of a pain for me.
    // it seems like when this is false, the player will first use p2p, then fall back to the cloud.
    // p2p seems to just use STUN to punch through the NAT, but so much is behind ssl that it's hard to
    // see what's going on. At some point, it would be good to dump the openssl keys and see what's going on.
    private fun isLanDevConnectable(): Boolean {
        return IoTVideoSdk.lanDevConnectable(PREFIX_THIRD_ID + cameraInfo.cameraId) == 1
    }

    fun release() {
        // Let the stop handler be graceful about it
        stop()

        // Should we call release? GC might actually be better
        iotVideoPlayer?.release()
        iotVideoPlayer = null
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Camera{").append("\n\t\t\t")
        sb.append("isFaulted=").append(isDecoderStalled).append("\n\t\t\t")
        sb.append("isSubscribed=").append(isSubscribed).append("\n\t\t\t")
        sb.append("framesProcessed=").append(framesProcessed)
        if (iotVideoPlayer != null) {
            sb.append("\n\t\t\t")
                .append("fromCamera: ${(iotVideoPlayer?.avBytesPerSec ?: 0) / 1024}kb/s")
        }
        sb.append("\n\t\t\t")
            .append("streamMuxer=").append(streamMuxer).append("\n\t\t\t")
        sb.append('}')
        return sb.toString()
    }

    companion object {
        private val TAG = RestreamingLifecycleHandler::class.simpleName
    }

}