package com.tencentcs.iotvideo

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tencentcs.iotvideo.custom.CameraCredential
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer
import com.tencentcs.iotvideo.ui.theme.CustomNativeIotVideoTheme
import com.tencentcs.iotvideo.utils.LogUtils
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

class CameraViewModel : ViewModel() {
    private val _cameraList = MutableLiveData<ArrayList<CameraPlayer>>()
    val cameraList: MutableLiveData<ArrayList<CameraPlayer>> = _cameraList

    private val _framesSent = MutableLiveData<Long>()
    val framesSent: MutableLiveData<Long> = _framesSent

    init {
        _cameraList.value = ArrayList()
        _framesSent.value = 0L
    }

    fun incrementFramesSent() {
        _framesSent.value = _framesSent.value?.plus(1)
    }

    fun addCamera(camera: CameraPlayer) {
        val currentList = _cameraList.value
        currentList?.add(camera)
        _cameraList.value = currentList
    }

    fun containsCamera(cameraId: String): Boolean {
        val currentList = _cameraList.value
        return currentList?.any { it.equalsCameraId(cameraId) } ?: false
    }

    fun popCamera(): CameraPlayer? {
        val currentList = _cameraList.value
        return currentList?.removeAt(0)
    }

    fun cameraCount(): Int {
        return _cameraList.value?.size ?: 0
    }

    fun activePortList(): List<Int> {
        val currentList = _cameraList.value
        return currentList?.map { it.port } ?: emptyList()
    }

    fun getStatusMessage(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("There are " + cameraCount() + " cameras active\n")
        for (camera in _cameraList.value!!) {
            stringBuilder.append(camera.toString())
        }
        stringBuilder.append("frames sent: " + framesSent.value)
        return stringBuilder.toString()
    }

}

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    private val TAG: String = "MainActivityIot"

    private var cryze_api: String = "http://cryze_api:8080"

    private val client = OkHttpClient()
    private val context = this // so i can pass it to the CameraViewer class... maybe not the best idea

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

        viewModel.cameraList.observe(this) { _ ->
            setContent{
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(viewModel.getStatusMessage())
                }
            }
        }

        viewModel.framesSent.observe(this) { _ ->
            setContent{
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(viewModel.getStatusMessage())
                }
            }
        }

        // start getting frames hooked up
        getCameraIdsFromServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        // stop all the threads
        while (viewModel.cameraCount() > 0)
        {
            val camera = viewModel.popCamera()
            camera?.playerThread?.interrupt()
        }
        unregisterIotVideoSdk()
    }

    private fun getCameraIdsFromServer(): Unit
    {
        val requestUrl = "$cryze_api/getCameraIds"
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

                if (!viewModel.containsCamera(loginInfo.deviceId))
                {
                    val cameraPlayer = CameraPlayer(loginInfo, context)

                    cameraPlayer.addOnFrameUpdateCallback {
                        runOnUiThread {
                            viewModel.incrementFramesSent()
                        }
                    }

                    // add the camera to the view model so the UI can display it
                    runOnUiThread {
                        viewModel.addCamera(cameraPlayer)
                    }

                    cameraPlayer.playerThread = Thread {
                        cameraPlayer.start()
                    }
                    cameraPlayer.playerThread?.start() ?: LogUtils.e(TAG, "Camera player thread is null")
                }
            }
        })
    }

    private fun unregisterIotVideoSdk()
    {
        IoTVideoSdk.unRegister()
        IoTVideoSdk.getMessageMgr().removeAppLinkListeners()
        IoTVideoSdk.getMessageMgr().removeModelListeners()
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