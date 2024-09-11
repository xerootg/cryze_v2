package com.github.xerootg.cryze.restreamer

import com.github.xerootg.cryze.MainActivity
import com.github.xerootg.cryze.httpclient.CryzeHttpClient
import com.github.xerootg.cryze.httpclient.responses.CameraInfo
import com.github.xerootg.cryze.restreamer.interfaces.ICameraStream
import com.tencent.mars.xlog.LogLevel
import com.tencent.mars.xlog.Xlog
import com.tencentcs.iotvideo.AppLinkState
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.IoTVideoSdk.SDK_REGISTER_STATE_REGISTERED
import com.tencentcs.iotvideo.StackTraceUtils
import com.tencentcs.iotvideo.iotvideoplayer.Mode
import com.tencentcs.iotvideo.iotvideoplayer.PlayerState
import com.tencentcs.iotvideo.messagemgr.IAppLinkListener
import com.tencentcs.iotvideo.messagemgr.MessageMgr
import com.tencentcs.iotvideo.messagemgr.SubscribeError
import com.tencentcs.iotvideo.messageparsers.DefaultModelMessageListener
import com.tencentcs.iotvideo.netconfig.NetConfig
import com.tencentcs.iotvideo.utils.LogUtils
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// There's some _mad_ hints in AIoTBaseRender about what the different values in AVData are
class RestreamingVideoPlayer(
    override val cameraInfo: CameraInfo,
    private val baseContext: MainActivity
) : ICameraStream {

    @Suppress("ktlint:standard:property-name")
    private val TAG = Companion.TAG + "::" + cameraInfo

    // TODO: make this a singleton. there's multiple copies and multiple instances of this info
    private val userInfo = hashMapOf<String, Any>(
        "IOT_HOST" to "|wyze-mars-asrv.wyzecam.com",
        "IOT_P2P_PORT_TYPE" to Mode.LAN.ordinal
    )

    @Synchronized
    override fun start() {

        // Ensure the SDK is initialized, only once.
        registrationLock.withLock {
            LogUtils.i(TAG, "Starting camera: $cameraInfo")
            if (!IoTVideoSdk.sdkInited) {
                IoTVideoSdk.init(baseContext.application, userInfo)
                val logPath = baseContext.filesDir.absolutePath
                IoTVideoSdk.setLogPath(logPath)

                // Verbose is.. around a hundreds logs per frame per camera.
                // debug is 2/3 of that, so still not useful.
                // info is about a dozen logs per frame per camera.
                // warning is still noisy but useful.
                val logLevel = LogLevel.LEVEL_WARNING

                IoTVideoSdk.setDebugMode(logLevel)
                Xlog.setConsoleLogOpen(true) // flushes xlog to the shared Log instance
                Xlog.setLogLevel(logLevel)
                Xlog.open(logLevel, Xlog.AppednerModeSync, logPath, logPath, "", "")

                LogUtils.i(TAG, "WYZE SDK initialized, log path: $logPath")
            }

            // All devices must register with the WYZE SDK before they can start streaming but only one needs to log in
            val shouldRegister = isSdkRegistered.compareAndSet(false, true)
            if (shouldRegister && IoTVideoSdk.registerState != SDK_REGISTER_STATE_REGISTERED) {
                LogUtils.i(TAG, "Registering with WYZE SDK")
                IoTVideoSdk.register(CryzeHttpClient.getAccessCredentialByCameraId(cameraInfo.cameraId))
            }


            // add our model listener
            MessageMgr.addModelListener(DefaultModelMessageListener(cameraInfo.cameraId))

            // if the app link is online, we can add the camera listener,
            // otherwise we need to wait for the app link to be online
            LogUtils.i(TAG, "Adding app link listener")
            MessageMgr.addAppLinkListener(appLinkHandler)
        }
        // Always start the device registration, the listener will handle future events. its entirely likely we missed the first online event
        addSubscribeDevice()
    }

    private var lastAppLinkState: AppLinkState = AppLinkState.APP_LINK_STATE_UNSET
    private val initialSubscribe = AtomicBoolean(false)
    private val appLinkHandler = object : IAppLinkListener {
        override fun onAppLinkStateChanged(state: AppLinkState?) {
            var resubscribeWithForce = false
            if (state == lastAppLinkState) {

                // if the device is healthy, we don't need to do anything
                if (_streamHandler?.isDecoderStalled != true
                    || _streamHandler?.isSubscribed == true
                ) {
                    LogUtils.i(TAG, "appLinkListener state did not change")
                    return
                } else {
                    // isDecoderStalled is set on a deadman timer. If it's set on a predefined interval
                    // we consider the camera is dead.
                    if (_streamHandler?.isDecoderStalled == true) {
                        LogUtils.i(TAG, "Reason: Decoder is stalled")
                    }
                    // isSubscribed is set when the camera is subscribed to the backend
                    if (_streamHandler?.isSubscribed != true) {
                        LogUtils.i(TAG, "Reason: Not subscribed")
                    }

                    // Should determine this if it
                    // if we are in the initial subscribe, no need to force
                    resubscribeWithForce = initialSubscribe.get()
                    LogUtils.i(TAG, "Resubscribing with force: $resubscribeWithForce")
                }

                if (resubscribeWithForce) {
                    // if the camera has a live stream, we need to actually stop it.
                    _streamHandler?.stop()
                }
            }
            lastAppLinkState = state ?: AppLinkState.APP_LINK_STATE_UNSET

            LogUtils.i(TAG, "appLinkListener state = $state")
            if (state == AppLinkState.APP_LINK_ONLINE) {

                if (_streamHandler?.isSubscribed == true && !resubscribeWithForce)
                    return // the stream is running apparently? don't subscribe

                // There's actually a reasonable chance this will fail to register the camera the first time.
                // if we don't call this, ironically the first camera will fail to render and the second camera will work.
                // there must be some sort of race in the SDK that causes this. The APP_LINK_ONLINE event will reconcile this.
                addSubscribeDevice(resubscribeWithForce)
                return
            }
            if (state == AppLinkState.APP_LINK_KICK_OFF ||
                state == AppLinkState.APP_LINK_TOKEN_EXPIRED ||
                (state?.value in
                        // A range of states where the token is invalid for one reason or another
                        AppLinkState.APP_LINK_DEV_REACTIVED.value..AppLinkState.APP_LINK_TERMID_INVALID.value)
            ) {
                LogUtils.w(TAG, "Token expired, unregistering and getting a new one")

                // reset the state so the listener lifecycle is not broken
                lastAppLinkState = AppLinkState.APP_LINK_STATE_UNSET

                Thread {
                    start()
                }.start()
                return
            }
        }
    }


    private fun unregisterSdk() {
        IoTVideoSdk.unRegister()
        MessageMgr.removeAppLinkListeners()
        MessageMgr.removeModelListeners()
    }

    // This is a crude singleton pattern. Only one camera can be set at a time.
    private var _streamHandler: RestreamingLifecycleHandler? = null
    private var streamHandler: RestreamingLifecycleHandler
        get() = synchronized(this) {
            _streamHandler ?: run {
                _streamHandler = object : RestreamingLifecycleHandler(cameraInfo) {
                    override fun onError(errorCode: Int, message: String) {
                        val errorType = SubscribeError.fromErrorCode(errorCode)
                        val logMessage = "onError: $errorCode, $errorType, $message"
                        if (!isSubscribed) {
                            LogUtils.e(TAG, "PreSubscribe: $logMessage")
                        } else {
                            LogUtils.w(TAG, "PostSubscribe: $logMessage")
                        }

                        // we probably failed to subscribe, so we need to resubscribe
                    }
                }
                _streamHandler!!
            }
        }
        // only one camera can be set at a time
        set(value) = synchronized(this) {
            _streamHandler = value
        }

    private fun addSubscribeDevice(force: Boolean = false) {
        initialSubscribe.set(true)
        if (streamHandler.playbackState == PlayerState.STATE_PLAY && !force) {
            LogUtils.i(TAG, "Camera is already playing, not subscribing")
            return
        }

        LogUtils.i(TAG, "Adding subscribe device")
        while (!NetConfig.trySubscribeDevice(
                CryzeHttpClient.getAccessCredentialByCameraId(cameraInfo.cameraId).accessToken,
                cameraInfo.cameraId,
                streamHandler,
                force
            )
        ) {
            LogUtils.i(TAG, "Failed to subscribe device, retrying")
            Thread.sleep(5_000)
        }

    }

    override fun stop() {
    }

    override fun release() {
        StackTraceUtils.logStackTrace(TAG, "release")

        stop()

        synchronized(streamHandler)
        {
            _streamHandler?.release()
            _streamHandler = null
        }
        unregisterSdk()

        // allow the next call to determine if the SDK is registered
        isSdkRegistered.set(false)
    }

    override fun hashCode(): Int {
        return cameraInfo.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RestreamingVideoPlayer

        return cameraInfo == other.cameraInfo
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("CameraPlayer{")
        stringBuilder.append("\n\tcameraId='").append(cameraInfo)
        stringBuilder.append("\n\tframesHandled=").append(streamHandler.framesProcessed)
        stringBuilder.append("\n\tstreamerState=").append(streamHandler.toString())
        stringBuilder.append("\n}")
        return stringBuilder.toString()
    }

    companion object {
        // Ensures only one camera can register at a time
        private val registrationLock = ReentrantLock()

        // Ensures the SDK is only registered once
        private val isSdkRegistered = AtomicBoolean(false)

        private val TAG = RestreamingVideoPlayer::class.simpleName
    }

}
