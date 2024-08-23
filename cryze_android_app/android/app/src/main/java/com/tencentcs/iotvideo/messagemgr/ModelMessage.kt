package com.tencentcs.iotvideo.messagemgr

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

open class ModelMessage(
    var device: String,
    id: Long,
    type: Int,
    error: Int,
    var path: String,
    var data: String
) : Message(type, id, error) {

    fun getPrettyMessage(): String {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val jsonElement = JsonParser.parseString(this.data).asJsonObject

        // Add additional values to the JSON object
        jsonElement.addProperty("device", this.device)
        jsonElement.addProperty("path", this.path)
        jsonElement.addProperty("error", this.error)
        jsonElement.addProperty("id", this.id)
        jsonElement.addProperty("type", MessageType.fromInt(this.type).name)

        return gson.toJson(jsonElement)
    }

    override fun toString(): String {
        return "ModelMessage{device='" + this.device +
                "', path='" +
                this.path +
                "', error='" +
                this.error +
                "', data='" +
                this.data +
                "' " + super.toString() + "}"
    }
}
