package com.tencentcs.iotvideo.messageparsers

import com.google.gson.Gson
import com.tencentcs.iotvideo.messagemgr.MessageType
import com.tencentcs.iotvideo.messagemgr.ModelMessage

data class Resolution(
    val width: Int,
    val height: Int
) {
    override fun toString(): String {
        return "Resolution(width=$width, height=$height)"
    }
}

data class ProductInfo(
    val productModel: String,
    val productID: String,
    val funcCode: Int,
    val revision: Int,
    val revisionUtc: Long,
    val resolution: Resolution,
    val videoCodec: String,
    val audioCodec: String,
    val streamChnNum: Int
) {
    override fun toString(): String {
        return "ProductInfo(productModel='$productModel', productID='$productID', funcCode=$funcCode, revision=$revision, revisionUtc=$revisionUtc, resolution=$resolution, videoCodec='$videoCodec', audioCodec='$audioCodec', streamChnNum=$streamChnNum)"
    }
}

data class VersionInfo(
    val sdkVer: String,
    val swVer: String,
    val hwVer: String
) {
    override fun toString(): String {
        return "VersionInfo(sdkVer='$sdkVer', swVer='$swVer', hwVer='$hwVer')"
    }
}

data class ProConstData(
    val firmwareMd5: String,
    val focus: Int,
    val _productInfo: ProductInfo,
    val _versionInfo: VersionInfo,
){
    override fun toString(): String {
        return "ProConstData(firmwareMd5='$firmwareMd5', focus=$focus, _productInfo=$_productInfo, _versionInfo=$_versionInfo)"
    }
}

class ProConstDeviceMessage(
    device: String,
    id: Long,
    type: MessageType,
    error: Int,
    path: String,
    data: String,
    val proConstData: ProConstData
): ModelMessage(device, id, type.ordinal, error, path, data) {
    override fun toString(): String {
        return "ProConstDeviceMessage(device='$device', path='$path', error=$error, id=$id, type='$type', data='$proConstData')"
    }

    companion object {
        const val MessagePath = "ProConst"

        fun fromModelMessage(message: ModelMessage): ProConstDeviceMessage {
            val gson = Gson()
            val data = gson.fromJson(message.data, ProConstData::class.java)
            return ProConstDeviceMessage(
                device = message.device,
                path = message.path,
                error = message.error,
                id = message.id,
                type = MessageType.fromInt(message.type),
                data = message.data,
                proConstData = data
            )
        }
    }
}
