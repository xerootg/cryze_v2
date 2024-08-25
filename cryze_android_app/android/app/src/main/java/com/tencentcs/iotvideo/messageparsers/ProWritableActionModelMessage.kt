package com.tencentcs.iotvideo.messageparsers

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tencentcs.iotvideo.messagemgr.MessageType
import com.tencentcs.iotvideo.messagemgr.ModelMessage

data class ActionData(
    @SerializedName("_otaVersion") val otaVersion: StVal? = null,
    @SerializedName("_otaUpgrade") val otaUpgrade: StVal? = null,
    @SerializedName("turnOff") val turnOff: StVal? = null,
    @SerializedName("fillLight") val fillLight: StVal? = null,
    @SerializedName("executeCmd") val executeCmd: StVal? = null,
    @SerializedName("siren") val siren: StVal? = null
)
{
    override fun toString(): String {
        val nonNullValues = listOfNotNull(
            otaVersion?.let { "_otaVersion=$it" },
            otaUpgrade?.let { "_otaUpgrade=$it" },
            turnOff?.let { "turnOff=$it" },
            fillLight?.let { "fillLight=$it" },
            executeCmd?.let { "executeCmd=$it" },
            siren?.let { "siren=$it" }
        )
        return "ActionData(${nonNullValues.joinToString(", ")})"
    }
}

class ProWriteActionModelMessage(
    device: String,
    id: Long,
    type: MessageType,
    error: Int,
    path: String,
    data: String,
    val dataObj: ActionData
): ModelMessage(device, id, type, error, path, data) {
    override fun toString(): String {
        return "ProWriteActionModelMessage(deviceId='$device', id=$id, type=$type, error=$error, path='$path', data=$dataObj)"
    }

    companion object {
        fun fromModelMessage(message: ModelMessage): ProWriteActionModelMessage {
            val gson = Gson()
            val data = gson.fromJson(message.data, ActionData::class.java)
            return ProWriteActionModelMessage(
                message.device,
                message.id,
                message.type,
                message.error,
                message.path,
                message.data,
                data)
        }

        // this is the only path I have seen in the wild
        const val MessagePath = "Action"
    }
}
