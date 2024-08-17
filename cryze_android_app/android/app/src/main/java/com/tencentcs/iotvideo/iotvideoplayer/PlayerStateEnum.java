package com.tencentcs.iotvideo.iotvideoplayer;

import org.jetbrains.annotations.NotNull;

public class PlayerStateEnum {
    public static final int STATE_IDLE = 0;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_LOADING = 4;
    public static final int STATE_PAUSE = 6;
    public static final int STATE_PLAY = 5;
    public static final int STATE_PREPARING = 2;
    public static final int STATE_READY = 3;
    public static final int STATE_SEEKING = 8;
    public static final int STATE_STOP = 7;

    @NotNull
    public static Object valueOf(int errorCode) {
        if (errorCode == STATE_IDLE) {
            return "STATE_IDLE";
        }
        if (errorCode == STATE_INITIALIZED) {
            return "STATE_INITIALIZED";
        }
        if (errorCode == STATE_LOADING) {
            return "STATE_LOADING";
        }
        if (errorCode == STATE_PAUSE) {
            return "STATE_PAUSE";
        }
        if (errorCode == STATE_PLAY) {
            return "STATE_PLAY";
        }
        if (errorCode == STATE_PREPARING) {
            return "STATE_PREPARING";
        }
        if (errorCode == STATE_READY) {
            return "STATE_READY";
        }
        if (errorCode == STATE_SEEKING) {
            return "STATE_SEEKING";
        }
        if (errorCode == STATE_STOP) {
            return "STATE_STOP";
        } else {
            return "STATE_UNKNOWN";
        }
    }
}
