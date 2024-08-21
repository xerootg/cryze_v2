package com.tencentcs.iotvideo.custom

import com.google.gson.Gson

enum class ServerType
{
    RTSP,
    MJPEG,
    RAW;

    companion object {
        fun fromValue(string: String): ServerType {
            when (string) {
                "RTSP" -> return RTSP
                "MJPEG" -> return MJPEG
                "RAW" -> return RAW
                else -> return RTSP
            }
        }
    }
}

data class CameraCredential(
    var accessId: Long,
    var accessToken: String,
    var deviceId: String,
    val expireTime: Long,
    val timestamp: Double,
    val socketPort: Int,
    val serverType: ServerType,
) {
    companion object {
        fun parseFrom(responseBody: String?): CameraCredential {
            // Parse the JSON string into a LoginInfoMessage object
            val gson = Gson()
            return gson.fromJson(responseBody, CameraCredential::class.java)
        }
    }
}
