package com.tencentcs.iotvideo

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tencentcs.iotvideo.custom.CameraCredential
import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
import com.tencentcs.iotvideo.iotvideoplayer.IConnectDevStateListener
import com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.ExportableVideoStreamDecoder
import com.tencentcs.iotvideo.iotvideoplayer.player.PlayerUserData
import com.tencentcs.iotvideo.messagemgr.MessageMgr
import com.tencentcs.iotvideo.ui.theme.CustomNativeIotVideoTheme
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.rxjava.IResultListener
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val userInfo = hashMapOf<String, Any>()
        userInfo["IOT_HOST"] = "|wyze-mars-asrv.wyzecam.com"
        IoTVideoSdk.init(this, userInfo)
    }
}


class MainActivity : ComponentActivity() {

    private val TAG: String = "MainActivityIot"

    private var cryze_api: String = "http://cryze_api:8080"

    private var iotVideoPlayer: IoTVideoPlayer = IoTVideoPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomNativeIotVideoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(":) Version: " + IoTVideoSdk.getP2PVersion())
                }
            }
        }

        getCameraIdsFromServer()
    }

    private fun getCameraIdsFromServer(): Unit
    {
        val requestUrl = "$cryze_api/getCameraIds"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(requestUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                throw IOException("Request failed: " + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                // the response is a json array of camera ids. parse it into a list
                val cameraIds = JSONArray(responseBody)
                for (i in 0 until cameraIds.length())
                {
                    val cameraId = cameraIds.getString(i)
                    LogUtils.i(TAG, "Camera ID: $cameraId")
                    getCameraCredentials(cameraId)
                }
            }
        })
    }

    private fun getCameraCredentials(cameraId: String): Unit
    {
        LogUtils.i(TAG, "Getting camera credentials for camera id: $cameraId")

        // get the camera credentials from the server
        val requestUrl = "$cryze_api/getToken?cameraId=$cameraId"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(requestUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
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

                // the response body is json serialized LoginInfoMessage, deserialize it
                val loginInfo = CameraCredential.parseFrom(responseBody)

                // then call the main login callback
                mainLoginCallback(loginInfo)
            }
        })
    }

    private var deviceIdList: ArrayList<String> = ArrayList()

    private fun registerIotVideoSdk(loginInfo: CameraCredential)
    {
        IoTVideoSdk.register(loginInfo.accessId, loginInfo.accessToken, 3)
        IoTVideoSdk.getMessageMgr().removeModelListeners()
        IoTVideoSdk.getMessageMgr().addModelListener {
            // do something with model info here?
            LogUtils.d("CLModelListener", "new model message! ${it.device}, path: ${it.path}, data: ${it.data}")
            deviceIdList.add(it.device)
        }
    }

    private fun unregisterIotVideoSdk()
    {
        IoTVideoSdk.unRegister()
        IoTVideoSdk.getMessageMgr().removeAppLinkListeners()
        IoTVideoSdk.getMessageMgr().removeModelListeners()
    }

    private fun addAppLinkListener(loginInfo: CameraCredential)
    {
        if (isSdkRegistered())
        {
            LogUtils.i(TAG, "appLinkListener() iot is register")
            addSubscribeDevice(loginInfo)
            return
        }

        IoTVideoSdk.getMessageMgr().addAppLinkListener {
            LogUtils.i(TAG, "appLinkListener state = $it")
            var shouldExit = false
            if (it == 1) {
                LogUtils.i(TAG, "Reg success, app online, start live")
                addSubscribeDevice(loginInfo)
                shouldExit = true
            }
            if (!shouldExit)
            {
                var z = true
                if (it != 6 && it != 13 && (12 > it || it >= 18))
                {
                    z = false
                }
                if (z)
                {
                    unregisterIotVideoSdk()
                }
            }

        }
    }

    private fun addSubscribeDevice(loginInfo: CameraCredential)
    {
        if (deviceIdList.contains(loginInfo.deviceId))
        {
            IoTVideoSdk.getNetConfig().subscribeDevice(loginInfo.accessToken, loginInfo.deviceId, object : IResultListener<Boolean> {
                override fun onError(i10: Int, str: String?) {
                    LogUtils.e("DeviceResultIOT", "on Error: $i10 with messsage: $str")
                }

                override fun onStart() {
                    LogUtils.e("DeviceResultIOT", "onStart")
                }

                override fun onSuccess(t10: Boolean?) {
                    LogUtils.e("DeviceResultIOT", "onSuccess: $t10")
                    var videoPlayer: IoTVideoPlayer = iotVideoPlayer
                    setupIotVideoPlayer(loginInfo)
                    videoPlayer.play() // start receiving packets
                    LogUtils.i(TAG, "Player state: ${videoPlayer.playState}");
                }
            })
        } else {
            LogUtils.w(TAG, "Deviceid not in device list of models so far")
            LogUtils.w(TAG, deviceIdList.toString())
        }
    }

    private fun isSdkRegistered() : Boolean
    {
        return MessageMgr.getSdkStatus() == 1
    }

    private fun setupIotVideoPlayer(loginInfo: CameraCredential)
    {
        val ioTVideoPlayer: IoTVideoPlayer = iotVideoPlayer

        ioTVideoPlayer.mute(true)
        ioTVideoPlayer.setDataResource("_@." + loginInfo.deviceId, 1, PlayerUserData(2))
        ioTVideoPlayer.setConnectDevStateListener(object : IConnectDevStateListener
        {
            override fun onStatus(i10: Int) {
                LogUtils.i(TAG, "onStatus for iotvideo player: $i10")
            }

        })
        ioTVideoPlayer.setAudioRender(object : IAudioRender{
            override fun flushRender() {
//                LogUtils.d(TAG, "IAudioRender flushRender for iotvideo player")
            }

            override fun getWaitRenderDuration(): Long {
                return 0L
            }

            override fun onFrameUpdate(aVData: AVData?) {
//                LogUtils.d(TAG, "IAudioRender onFrameUpdate for iotvideo player, size: ${aVData.toString()}")
            }

            override fun onInit(aVHeader: AVHeader?) {
//                LogUtils.d(TAG, "IAudioRender override fun onInit(aVHeader: AVHeader?) for iotvideo player, size: ${aVHeader.toString()}")
            }

            override fun onRelease() {
//                LogUtils.d(TAG, "IAudioRender onRelease for iotvideo player")
            }

            override fun setPlayerVolume(f10: Float) {
//                LogUtils.d(TAG, "IAudioRender setPlayerVolume for iotvideo player}")
            }

        })

        // Setting the video render will only get a frame if you use a decoder in android like MediaCodecVideoDecoder,
        // if you use a custom IVideoDecoder then nothing will get sent to onFrameUpdate
        ioTVideoPlayer.setVideoRender(object : IVideoRender {
            override fun onFrameUpdate(aVData: AVData?) {
                LogUtils.d(TAG, "CustomVideoRender onFrameUpdate for iotvideo player, size: ${aVData.toString()}")
            }

            override fun onInit(aVHeader: AVHeader?) {
                LogUtils.d(TAG, "IVideoRender override fun onInit(aVHeader: AVHeader?) {\n for iotvideo player, size: ${aVHeader.toString()}")
            }

            override fun onPause() {
//                LogUtils.d(TAG, "IVideoRender onPause for iotvideo player")
            }

            override fun onRelease() {
//                LogUtils.d(TAG, "IVideoRender onRelease for iotvideo player")
            }

            override fun onResume() {
//                LogUtils.d(TAG, "IVideoRender onResume for iotvideo player")
            }

        })

        //ioTVideoPlayer.setVideoDecoder(MediaCodecVideoDecoder())
        ioTVideoPlayer.setVideoDecoder(ExportableVideoStreamDecoder(loginInfo.socketPort, this))

        ioTVideoPlayer.setErrorListener { errorNumber ->
            LogUtils.i(
                TAG,
                "errorListener onError for iotvideo player: $errorNumber"
            )
        }
        ioTVideoPlayer.setStatusListener { statusCode ->
            LogUtils.i(
                TAG,
                "IStatusListener onStatus for iotvideo player: $statusCode"
            )
        }
    }

    private fun mainLoginCallback(loginInfo: CameraCredential)
    {
            LogUtils.e(TAG, "LOGIN INFO accessId: ${loginInfo.accessId}, token: ${loginInfo.accessToken}, deviceId: ${loginInfo.deviceId}")
            registerIotVideoSdk(loginInfo)
            Thread.sleep(2200)
            addAppLinkListener(loginInfo)
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CustomNativeIotVideoTheme {
        Greeting("Android")
    }
}
