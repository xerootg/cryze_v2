package com.tencentcs.iotvideo.iotvideoplayer

interface IStatusListener {

    // JNI uses this, I want Enums in Kotlin
    @Suppress("unused")
    fun onStatus(code: Int){
        onStatus(PlayerState.fromInt(code))
    }

    fun onStatus(state: PlayerState){}
}
