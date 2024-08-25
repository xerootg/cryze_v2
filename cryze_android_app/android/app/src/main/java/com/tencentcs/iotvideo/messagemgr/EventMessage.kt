package com.tencentcs.iotvideo.messagemgr

class EventMessage(type: MessageType, var topic: String, var data: String) : Message(type, 0L, 0) {
    override fun toString(): String {
        val sb2 = StringBuilder("EventMessage{topic='")
        sb2.append(this.topic)
        sb2.append("', data='")
        sb2.append(this.data)
        sb2.append("'}")
        return sb2.toString()
    }
}
