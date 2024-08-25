package com.tencentcs.iotvideo.messageparsers

import com.google.gson.Gson
import com.tencentcs.iotvideo.messagemgr.MessageType
import com.tencentcs.iotvideo.messagemgr.ModelMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NetInfoStVal(
    val type: Int,
    val signalQuality: Int,
    val ssid: String,
    val ip: String,
    val mac: String
){
    override fun toString(): String {
        return "NetInfoStVal(type=$type, signalQuality=$signalQuality, ssid='$ssid', ip='$ip', mac='$mac')"
    }
}

data class ProReadonlyNetInfo(
    val t: Long,
    val stVal: NetInfoStVal
){
    override fun toString(): String {
        val dateTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
            Date(t * 1000)
        )
        return "ProReadonlyNetInfo(t=$dateTime, stVal=$stVal)"
    }
}

class ProReadonlyNetInfoModelMessage(
    device: String,
    id: Long,
    type: MessageType,
    error: Int,
    path: String,
    data: String,
    val netInfo: ProReadonlyNetInfo
): ModelMessage(device, id, type, error, path, data)
{
    override fun toString(): String {
        return "ProReadonlyNetInfoModelMessage(device='$device', id=$id, type=$type, error=$error, path='$path', data='$data', netInfo=$netInfo)"
    }
    companion object{
        const val MessagePath = "ProReadonly.netInfo"

        fun fromModelMessage(msg: ModelMessage): ProReadonlyNetInfoModelMessage{
            val data = Gson().fromJson(msg.data, ProReadonlyNetInfo::class.java)
            return ProReadonlyNetInfoModelMessage(
                msg.device,
                msg.id,
                msg.type,
                msg.error,
                msg.path,
                msg.data,
                data)
        }
    }
}
