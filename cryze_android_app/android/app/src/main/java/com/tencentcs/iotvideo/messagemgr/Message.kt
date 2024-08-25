package com.tencentcs.iotvideo.messagemgr

open class Message(@JvmField var type: MessageType, @JvmField var id: Long, @JvmField var error: Int) {
    override fun toString(): String {
        return "type: " + this.type + ", id: " + this.id + ", error: " + this.error
    }
}
