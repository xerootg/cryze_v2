package com.tencentcs.iotvideo.iotvideoplayer

enum class PlayerState(val code: Int) {
    STATE_IDLE(0),
    STATE_INITIALIZED(1),
    STATE_PREPARING(2),
    STATE_READY(3),
    STATE_LOADING(4),
    STATE_PLAY(5),
    STATE_PAUSE(6),
    STATE_STOP(7),
    STATE_SEEKING(8),
    STATE_UNKNOWN(-1);

    companion object {
        @JvmStatic
        fun fromInt(errorCode: Int): PlayerState {
            for (state in entries) {
                if (state.code == errorCode) {
                    return state
                }
            }
            return STATE_UNKNOWN
        }
    }
}
