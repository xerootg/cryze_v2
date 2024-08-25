package com.tencentcs.iotvideo.iotvideoplayer;

public interface IPreparedListener {
    // JNI throws if this interface is missing
    @SuppressWarnings("unused")
    void onPrepared();
}
