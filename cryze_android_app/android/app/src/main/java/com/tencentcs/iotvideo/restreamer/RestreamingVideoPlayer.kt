package com.tencentcs.iotvideo.restreamer

import com.tencentcs.iotvideo.AppLinkState
import com.tencentcs.iotvideo.BuildConfig.CRYZE_BACKEND_URL
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.IoTVideoSdk.DEV_TYPE_THIRD_ID
import com.tencentcs.iotvideo.IoTVideoSdk.LOG_LEVEL_DEBUG
import com.tencentcs.iotvideo.IoTVideoSdk.SDK_REGISTER_STATE_REGISTERED
import com.tencentcs.iotvideo.MainActivity
import com.tencentcs.iotvideo.StackTraceUtils
import com.tencentcs.iotvideo.custom.CameraCredential
import com.tencentcs.iotvideo.iotvideoplayer.Mode
import com.tencentcs.iotvideo.messagemgr.IAppLinkListener
import com.tencentcs.iotvideo.messagemgr.MessageMgr
import com.tencentcs.iotvideo.messageparsers.DefaultModelMessageListener
import com.tencentcs.iotvideo.restreamer.interfaces.ICameraStream
import com.tencentcs.iotvideo.utils.LogUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

// There's some _mad_ hints in AIoTBaseRender about what the different values in AVData are
class RestreamingVideoPlayer(override val cameraId: String, private val baseContext: MainActivity) :
    ICameraStream
{
    val TAG = RestreamingVideoPlayer::class.simpleName+"::"+cameraId

    var cameraCredential : CameraCredential? = null

    private val userInfo = hashMapOf<String, Any>(
        "IOT_HOST" to "|wyze-mars-asrv.wyzecam.com",
        "IOT_P2P_PORT_TYPE" to Mode.LAN.ordinal
    )

    @Synchronized
    private fun refreshCameraCredentials(): Unit
    {
        if(cameraCredential != null) {
            // find the offender and fix it if this happens. This should never happen.
            throw IllegalStateException("Camera credentials already set")
        }

        var requestCompleted = false;
        LogUtils.i(TAG, "Getting camera credentials for camera id: $cameraId")

        // get the camera credentials from the server
        val requestUrl = "$CRYZE_BACKEND_URL/getToken?cameraId=$cameraId"
        val request = Request.Builder()
            .url(requestUrl)
            .build()

        val requestHandler = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                throw IOException("Request failed: " + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty())
                {
                    LogUtils.e(TAG, "Did not get a response body from the server")
                    return
                }

                // log the response body
                LogUtils.i(TAG, "Response body: $responseBody")

                if (!response.isSuccessful) {
                    LogUtils.e(TAG, "Request failed with status code: ${response.code}")
                    throw IOException("Request failed with status code: ${response.code} and message: ${response.message}")
                }

                // the response body is json serialized LoginInfoMessage, deserialize it
                cameraCredential = CameraCredential.parseFrom(responseBody)
                requestCompleted = true
            }
        }

        baseContext.client.newCall(request).enqueue(requestHandler)

        // wait for the request to complete
        while (!requestCompleted) {
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                LogUtils.i(TAG, "Camera credentials request interrupted")
            }
        }
        LogUtils.i(TAG, "Got camera credentials for camera id: $cameraId")
    }

    private var pauseWatchdog = false
    private var watchdog = Thread {
        try{
            while (!Thread.currentThread().isInterrupted) {
                if (pauseWatchdog) {
                    Thread.sleep(1000)
                    continue
                }
                if (camera.isDecoderStalled) {
                    LogUtils.e(TAG, "Camera is faulted, Attempting to recover")
                    _restreamerLifecycleHandler?.release()
                    _restreamerLifecycleHandler = null // release the camera
                    start() // The SDK should handle the correct events
                }

                Thread.sleep(1000)
            }
        } catch (_: InterruptedException) {}
    }

    @Synchronized
    override fun start() {
        if (cameraCredential == null) {
            LogUtils.i(TAG, "Camera start called without camera credentials, refreshing")
            refreshCameraCredentials()
        }
        LogUtils.i(TAG, "Starting camera!")

        if(!IoTVideoSdk.isInited())
        {
            IoTVideoSdk.init(baseContext.application, userInfo)
            val logPath = baseContext.getExternalFilesDir(null)?.absolutePath
            IoTVideoSdk.setLogPath(logPath)
            IoTVideoSdk.setDebugMode(LOG_LEVEL_DEBUG)
            LogUtils.i(TAG, "WYZE SDK initialized, log path: $logPath")
        }

        // All devices must register with the WYZE SDK before they can start streaming but only one needs to log in
        val thisCameraCredential = cameraCredential!!
        if (IoTVideoSdk.getRegisterState() != SDK_REGISTER_STATE_REGISTERED) {
            LogUtils.i(TAG, "Registering with WYZE SDK")
            IoTVideoSdk.register(
                thisCameraCredential.accessId,
                thisCameraCredential.accessToken,
                DEV_TYPE_THIRD_ID
            )
            IoTVideoSdk.getMessageMgr().removeModelListeners()
            LogUtils.i(TAG, "Removed existing model listeners")
        }

        // add our model listener
        IoTVideoSdk.getMessageMgr().addModelListener(DefaultModelMessageListener(cameraId))

        // if the app link is online, we can add the camera listener,
        // otherwise we need to wait for the app link to be online
        val messageMgr = IoTVideoSdk.getMessageMgr()
        if (MessageMgr.getSdkStatus() != AppLinkState.APP_LINK_ONLINE) {
            messageMgr.removeAppLinkListeners()
        } else {
            LogUtils.i(TAG, "App link already online, registering listeners and starting")
        }

        // add our app link listener so we know when to tear down the SDK
        // and when we can add our camera listener.
        // messages are not repeated
        LogUtils.i(TAG, "Adding app link listener")
        messageMgr.addAppLinkListener(appLinkHandler)

        // Always start the device registration, the listener will handle future events. its entirely likely we missed the first online event
        addSubscribeDevice()
    }

    private val appLinkHandler = object : IAppLinkListener {
        override fun onAppLinkStateChanged(state: AppLinkState?) {
            LogUtils.i(TAG, "appLinkListener state = $state")
            if (state == AppLinkState.APP_LINK_ONLINE) {
                LogUtils.i(
                    TAG,
                    "Reg success, app online, start live"
                )

                // There's actually a reasonable chance this will fail to register the camera the first time.
                // if we don't call this, ironically the first camera will fail to render and the second camera will work.
                // there must be some sort of race in the SDK that causes this.
                addSubscribeDevice()
                return
            }
            if (state == AppLinkState.APP_LINK_KICK_OFF ||
                state == AppLinkState.APP_LINK_TOKEN_EXPIRED ||
                (state?.value in
                        // A range of states where the token is invalid for one reason or another
                        AppLinkState.APP_LINK_DEV_REACTIVED.value..AppLinkState.APP_LINK_TERMID_INVALID.value)
                ) {
                LogUtils.w(TAG, "Token expired, unregistering and getting a new one")

                // queue up a refresh. This will cause the credentials to re-register w/ the backend and then start the camera
                unregisterSdk()
                cameraCredential = null

                // we are dead, really. remove the app link listener and start over.
                IoTVideoSdk.getMessageMgr().removeAppLinkListener(this)

                Thread {
                    start()
                }.start()
                return
            }
        }
    }


    private fun unregisterSdk()
    {
        IoTVideoSdk.unRegister()
        val messageMgr = IoTVideoSdk.getMessageMgr()
        messageMgr.removeAppLinkListeners()
        messageMgr.removeModelListeners()
    }

    private var _restreamerLifecycleHandler: RestreamingLifecycleHandler? = null

    var camera: RestreamingLifecycleHandler
        get() = synchronized(this){
            _restreamerLifecycleHandler ?: run {
                if (cameraCredential == null) {
                    LogUtils.i(TAG, "Camera construction attempted without camera credentials: $cameraId")
                    refreshCameraCredentials()
                }
                _restreamerLifecycleHandler = RestreamingLifecycleHandler(cameraCredential!!, baseContext)
                _restreamerLifecycleHandler!!
            }
        }
        // only one camera can be set at a time
        set(value) = synchronized(this){
            if(cameraCredential == null) {
                _restreamerLifecycleHandler = value
            }
        }

    private fun addSubscribeDevice()
    {
        if(cameraCredential == null || cameraCredential?.isUsed == true)
        {
            cameraCredential= null
            refreshCameraCredentials()
        }

        LogUtils.i(TAG, "Adding subscribe device")
        cameraCredential?.isUsed = true // consume it
        IoTVideoSdk.getNetConfig()
            .subscribeDevice(cameraCredential?.accessToken, cameraCredential?.deviceId, camera)

    }

    override fun stop() {
    }

    override fun release()
    {
        StackTraceUtils.logStackTrace(TAG, "release")

        // kill all the resources we use
        pauseWatchdog = true
        watchdog.interrupt()
        synchronized(camera)
        {
            _restreamerLifecycleHandler?.release()
            _restreamerLifecycleHandler = null
        }
        unregisterSdk()
    }

    override fun hashCode(): Int{
        return cameraId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RestreamingVideoPlayer

        return cameraId == other.cameraId
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("CameraPlayer{")
        stringBuilder.append("\n\tcameraId='").append(cameraCredential?.deviceId)
        stringBuilder.append("\n\tframesHandled=").append(camera.framesProcessed)
        if(cameraCredential != null) {
            stringBuilder.append("\n\tstreamerState=").append(camera.toString())
        } else {
            stringBuilder.append("\n\tstreamerState= Waiting for Credentials")
        }
        stringBuilder.append("\n}")
        return stringBuilder.toString()
    }

}