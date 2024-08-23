package com.tencentcs.iotvideo.iotvideoplayer

enum class Mode(val value: Int) {
    LAN(3),
    P2P(2),
    RELAY(1),
    UNINITIALIZED(-1),
    UNKNOWN(0);

    companion object {
        fun fromValue(value: Int): Mode {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}

enum class Protocol(val value: Int) {
    TCP(1),
    UDP(0),
    UNKNOWN(-1);

    companion object {
        fun fromValue(value: Int): Protocol {
            return values().find { it.value == value } ?: UNKNOWN
        }
    }
}

class ConnectMode {
    @JvmField
    var mMode: Int = 0
    @JvmField
    var mProtocol: Int = -1

    override fun toString(): String {
        val sb2 = StringBuilder("ConnectMode{mMode=")
        sb2.append(Mode.fromValue(this.mMode))
        sb2.append(", mProtocol=")
        sb2.append(Protocol.fromValue(this.mProtocol))
        sb2.append('}')
        return sb2.toString()
    }
}
