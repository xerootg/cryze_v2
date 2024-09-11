package com.github.xerootg.cryze.httpclient.responses

import com.google.gson.Gson

data class AccessCredential(
    val accessId: Long,
    val accessToken: String
) {
    companion object {
        fun parseFrom(responseBody: String): AccessCredential {
            return Gson().fromJson(responseBody, AccessCredential::class.java)
        }
    }
}