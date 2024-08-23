package com.tencentcs.iotvideo.messageparsers

import com.google.gson.Gson
import com.tencentcs.iotvideo.messagemgr.MessageType
import com.tencentcs.iotvideo.messagemgr.ModelMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StVal(
    val stVal: Any?, // sometimes its empty string, sometimes its an int, sometimes its null
    val t: Long?,
    val mode: Int?,
    val charging: Int?,
    val battery: Int?
) {
    override fun toString(): String {
        val nonNullValues = listOfNotNull(
            stVal?.let { "stVal=$it" },
            t?.let {
                if(it > 966984138){ // some random time along time ago
                val dateTime =SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date(it * 1000)
                )
                "DateTime=$dateTime"} else "UnknownT=$it" },
            mode?.let { "mode=$it" },
            charging?.let {
                val charging = if (it == 0) "false" else "true"
                "charging=$charging" },
            battery?.let { "batteryLevel=$it" } // unknown scale, the wyze app reports 84 as 100 im pretty sure
        )
        return "StVal(${nonNullValues.joinToString(", ")})"
    }
}

data class PowerData(
    val t: Long,
    val stVal: StVal
) {
//    val dateTime: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(t), ZoneId.systemDefault())
    val dateTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(t * 1000))

    override fun toString(): String {
        return "PowerData(t=$t, dateTime=$dateTime, stVal=$stVal)"
    }
}

data class ProReadonlyModelMessage(
    val device: String,
    val path: String,
    val error: Int,
    val id: Long, // the JNI made me do it
    val type: MessageType,
    val data: PowerData
) {
    override fun toString(): String {
        return "ProConstDeviceMessage(device='$device', path='$path', error=$error, id=$id, type='$type', data='$data')"
    }

    companion object {
        val MessagePath = "ProReadonly"
        fun fromModelMessage(message: ModelMessage): ProReadonlyModelMessage {
            val gson = Gson()
            val data = gson.fromJson(message.data, PowerData::class.java)
            return ProReadonlyModelMessage(
                device = message.device,
                path = message.path,
                error = message.error,
                id = message.id,
                type = MessageType.fromInt(message.type),
                data = data
            )
        }
    }
}
