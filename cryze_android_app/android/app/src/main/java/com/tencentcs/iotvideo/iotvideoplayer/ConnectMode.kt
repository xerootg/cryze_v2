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
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}

class ConnectMode(connectionMode: IntArray?) {
    // Leave this open (not private) for future use
    var mMode = Mode.UNKNOWN
        private set

    // Leave this open (not private) for future use
    var mProtocol = Protocol.UNKNOWN
        private set

    init {
        if (connectionMode != null && connectionMode.size == 3) {
            val i = connectionMode[0]
            if (i == 0) {
                this.mMode = Mode.fromValue(connectionMode[1])
                this.mProtocol = Protocol.fromValue(connectionMode[2])
            } else {
                this.mMode = Mode.fromValue(i)
            }
        }
    }

    override fun toString(): String {
        val sb2 = StringBuilder("ConnectMode{mMode=")
        sb2.append(this.mMode)
        sb2.append(", mProtocol=")
        sb2.append(this.mProtocol)
        sb2.append('}')
        return sb2.toString()
    }
}
