package com.tencentcs.iotvideo.iotvideoplayer;

// TODO: replace int with the correct Enum type
public interface IConnectDevStateListener {

    // JNI uses this, I want to use Enums in our code.
    @SuppressWarnings("unused")
    default void onStatus(int statusCode){
        onStatus(PlayerState.fromInt(statusCode));
    }

    void onStatus(PlayerState statusCode);
}
