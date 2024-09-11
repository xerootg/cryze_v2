package com.github.xerootg.cryze.httpclient

import android.os.Handler
import android.os.HandlerThread
import com.github.xerootg.cryze.BuildConfig.CRYZE_BACKEND_URL
import com.github.xerootg.cryze.httpclient.responses.AccessCredential
import com.github.xerootg.cryze.httpclient.responses.CameraInfo
import com.tencentcs.iotvideo.messagemgr.ModelMessage
import com.tencentcs.iotvideo.utils.LogUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray

// wrapper for async http requests
private data class ClientResponse<T>(
    private val exception: Exception?,
    private val response: T?,
    private val success: Boolean = false
) {
    fun getValue(): T {
        if (success) {
            if(response != null) {
                return response
            }
            throw Exception("Response is null")
        }
        throw exception!!
    }
}

object CryzeHttpClient {

    private val mTaskThread: HandlerThread = HandlerThread("HttpSender")
    private val mTaskTHandler: Handler by lazy { Handler(mTaskThread.looper) }

    init {
        mTaskThread.start()
    }
    //TODO: figure out how to properly close the thread
//    destructor
//    {
//        mTaskThread.quit()
//    }

    // tokens must be consumed immediately and not held
    private fun getCameraCredential(cameraId: String): ClientResponse<AccessCredential> {
        try {
            val request = Request.Builder()
                .url("$SERVER/Camera/CameraToken?deviceId=$cameraId")
                .build()
            val response = instance.newCall(request).execute()
            val code = response.code
            if (code != 200) {
                throw Exception("Failed to get camera credential: $code")
            }
            val body = response.body?.string() ?: throw Exception("Failed to get camera credential")
            LogUtils.d(CryzeHttpClient::class.simpleName, "getCameraCredential: Body: $body")

            return ClientResponse(
                response = AccessCredential.parseFrom(body),
                success = true,
                exception = null
            )
        } catch (e: Exception) {
            return ClientResponse(exception = e, success = false, response = null)
        }
    }

    fun getAccessCredentialByCameraId(cameraId: String): AccessCredential {
        var tokenResult: ClientResponse<AccessCredential>? = null
        if (!mTaskTHandler.post {
                tokenResult = getCameraCredential(cameraId)
            }) {
            throw Exception("Failed to get camera credential, failed to post")
        }
        while (tokenResult == null) {
            Thread.sleep(100)
        }

        return tokenResult!!.getValue()
    }

    private fun getCamerasInternal(): ClientResponse<List<String>> {
        try {
            val request = Request.Builder()
                .url("$SERVER/Camera/CameraList")
                .build()
            val response = instance.newCall(request).execute()
            val code = response.code

            // it'll get wrapped in an exception anyway
            if (code != 200) {
                throw Exception("Failed to get cameras: $code")
            }
            val body = response.body?.string() ?: throw Exception("Failed to get cameras")
            LogUtils.d(CryzeHttpClient::class.simpleName, "getCameras: Body: $body")

            // Response is literally a list of camera ids (string!)

            val cameraIds = JSONArray(body)
            val cameraList = mutableListOf<String>()
            for (i in 0 until cameraIds.length()) {
                cameraList.add(cameraIds.getString(i))
            }
            return ClientResponse(response = cameraList, success = true, exception = null)
        } catch (e: Exception) {
            return ClientResponse(exception = e, success = false, response = null)
        }
    }

    fun getCameras(): List<String> {
        var response: ClientResponse<List<String>>? = null
        if (!mTaskTHandler.post {
                response = getCamerasInternal()
            }) {
            throw Exception("Failed to get cameras, failed to post")
        }
        while (response == null) {
            Thread.sleep(100)
        }

        return response!!.getValue()
    }

    fun postCameraModelMessage(cameraId: String, message: ModelMessage) {
        val data = message.data
        val messageType = message.type.name
        val path = message.path
        val request = Request.Builder()
            .url("$SERVER/CameraMessage?cameraId=$cameraId&messageType=$messageType&path=$path")
            .post(data.toRequestBody("application/json".toMediaType()))
            .build()

        if (!mTaskTHandler.post {
                // frankly, I don't care about the response. It'll probably
                // eventually be important, but not now.
                instance.newCall(request).execute()
            }) {
            throw Exception("Failed to put message, failed to post")
        }
    }

    private fun getCameraInfoInternal(cameraId: String): ClientResponse<CameraInfo> {
        try {
            val request = Request.Builder()
                .url("$SERVER/Camera/DeviceInfo?deviceId=$cameraId")
                .build()
            val response = instance.newCall(request).execute()
            val code = response.code
            // it'll get wrapped in an exception anyway
            if (code != 200) {
                throw Exception("Failed to get camera info: $code")
            }
            val body = response.body?.string() ?: throw Exception("Failed to get camera info")
            LogUtils.d(CryzeHttpClient::class.simpleName, "getCameraInfo: Body: $body")
            return ClientResponse(response = CameraInfo.parseFrom(body), success = true, exception = null)
        } catch (e: Exception) {
            return ClientResponse(exception = e, success = false, response = null)
        }
    }

    fun getCameraInfo(it: String): CameraInfo {
        var response: ClientResponse<CameraInfo>? = null
        if (!mTaskTHandler.post {
                response = getCameraInfoInternal(it)
            }) {
            throw Exception("Failed to get camera info, failed to post")
        }
        while (response == null) {
            Thread.sleep(100)
        }

        return response!!.getValue()
    }

    private const val SERVER = CRYZE_BACKEND_URL
    private val instance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .build()
    }
}
