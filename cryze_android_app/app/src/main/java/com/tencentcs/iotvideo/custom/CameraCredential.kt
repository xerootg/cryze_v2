package com.tencentcs.iotvideo.custom

import com.google.gson.Gson

data class CameraCredential(
    var accessId: Long,
    var accessToken: String,
    var deviceId: String,
    val expireTime: Long,
    val timestamp: Double,
    val socketPort: Int
) {
    companion object {
        fun parseFrom(responseBody: String?): CameraCredential {
            // Parse the JSON string into a LoginInfoMessage object
            val gson = Gson()
            return gson.fromJson(responseBody, CameraCredential::class.java)
        }
    }
}
