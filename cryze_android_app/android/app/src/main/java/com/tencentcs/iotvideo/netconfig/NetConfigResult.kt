package com.tencentcs.iotvideo.netconfig

import androidx.core.app.NotificationCompat
import com.google.gson.annotations.SerializedName

/* loaded from: classes12.dex */
class NetConfigResult {
    @SerializedName("devId")
    var devId: String? = null

    @SerializedName(NotificationCompat.CATEGORY_STATUS)
    var status: Int = 0

    @SerializedName("token")
    var token: String? = null

    override fun toString(): String {
        return "NetConfigResult{token='" + this.token + "'" + ", devId='" + this.devId + "'" + ", status=" + this.status + "}"
    }

    companion object {
        const val STATUS_FAILED: Int = 0
        const val STATUS_WAITING: Int = 1
    }
}