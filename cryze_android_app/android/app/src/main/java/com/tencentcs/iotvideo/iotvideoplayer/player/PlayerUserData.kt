package com.tencentcs.iotvideo.iotvideoplayer.player

class PlayerUserData {
    var definition: Int
    var fileStartTime: Long
    var offset: Int = 0
    var playbackTime: Long
    var sourceId: Short

    constructor(definition: Int) {
        this.definition = definition
        this.playbackTime = 0L
        this.fileStartTime = 0L
        this.sourceId = 0.toShort()
    }

    override fun toString(): String {
        val sb2 = StringBuilder("PlayerUserData{definition=")
        sb2.append(this.definition)
        sb2.append(", playbackTime=")
        sb2.append(this.playbackTime)
        sb2.append(", fileStartTime=")
        sb2.append(this.fileStartTime)
        sb2.append(", sourceId=")
        sb2.append(sourceId.toInt())
        sb2.append(", offset=")
        sb2.append(this.offset)
        sb2.append('}')
        return sb2.toString()
    }

    constructor(definition: Int, sourceId: Short) {
        this.definition = definition
        this.playbackTime = 0L
        this.fileStartTime = 0L
        this.sourceId = sourceId
    }

    constructor(definition: Int, pBackTime: Long, fStartTime: Long) {
        this.definition = definition
        this.playbackTime = pBackTime
        this.fileStartTime = fStartTime
        this.sourceId = 0.toShort()
    }

    constructor() {
        this.definition = 0
        this.playbackTime = 0L
        this.fileStartTime = 0L
        this.sourceId = 0.toShort()
    }
}
