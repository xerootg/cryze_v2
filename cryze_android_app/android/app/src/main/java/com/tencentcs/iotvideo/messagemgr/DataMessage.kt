package com.tencentcs.iotvideo.messagemgr

class DataMessage(id: Long, type: MessageType, error: Int, var data: ByteArray) : Message(type, id, error) {
    override fun toString(): String {
        val sb2 = StringBuilder("DataMessage{data=")
        sb2.append(data.contentToString())
        sb2.append(", type=")
        sb2.append(this.type)
        sb2.append(", id=")
        sb2.append(this.id)
        sb2.append(", error=")
        sb2.append(this.error)
        sb2.append('}')
        return sb2.toString()
    }
}
