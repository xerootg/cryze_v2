package com.github.xerootg.cryze.httpclient.responses

import com.google.gson.Gson

data class CameraInfo(
    val cameraId: String,
    val streamName: String?
){
    companion object {
        fun parseFrom(responseBody: String): CameraInfo {
            return Gson().fromJson(responseBody, CameraInfo::class.java)
        }
    }
}