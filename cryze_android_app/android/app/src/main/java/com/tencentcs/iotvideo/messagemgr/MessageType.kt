package com.tencentcs.iotvideo.messagemgr

enum class MessageType(type: Int) {
    MSG_TYPE_EVENT(0),
    MSG_TYPE_PRO_CONST(1),
    MSG_TYPE_PRO_READONLY(2),
    MSG_TYPE_PRO_WRITABLE(3),
    MSG_TYPE_ACTION(4),
    MSG_TYPE_UNKNOWN(-1); // not official, but this list is incomplete

    companion object {
        fun fromInt(type: Int): MessageType {
            return entries.find { it.ordinal == type } ?: MSG_TYPE_UNKNOWN
        }
    }
}
