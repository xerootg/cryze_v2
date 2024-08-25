package com.tencentcs.iotvideo.messageparsers

import com.google.gson.Gson
import com.tencentcs.iotvideo.messagemgr.MessageType
import com.tencentcs.iotvideo.messagemgr.ModelMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PowerStVal(
    val mode: Int,
    val charging: Int,
    val battery: Int
)
{
    override fun toString(): String {
        val isCharging = charging == 1
        return "PowerStVal(mode=$mode, charging=$isCharging, battery=$battery)"
    }
}

data class ProReadonlyPower(
    val t: Long,
    val stVal: PowerStVal
){
    override fun toString(): String {
        val dateTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
            Date(t * 1000)
        )
        return "ProReadonlyPower(TimeOfSample=$dateTime, Reading=$stVal)"
    }
}

class ProReadonlyPowerModelMessage(
    device: String,
    id: Long,
    type: MessageType,
    error: Int,
    path: String,
    data: String,
    val power: ProReadonlyPower
): ModelMessage(device, id, type, error, path, data) {

    override fun toString(): String {
        return "ProReadonlyPowerModelMessage(device='$device', id=$id, type=$type, error=$error, path='$path', power=$power)"
    }

    companion object {
        fun fromModelMessage(message: ModelMessage): ProReadonlyPowerModelMessage {
            val gson = Gson()
            val data = gson.fromJson(message.data, ProReadonlyPower::class.java)
            return ProReadonlyPowerModelMessage(
                message.device,
                message.id,
                message.type,
                message.error,
                message.path,
                message.data,
                data
            )
        }

        const val MessagePath = "ProReadonly.power"
    }
}
