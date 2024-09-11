package com.tencentcs.iotvideo.utils.rxjava

// Generically, all requests to the IoTVideoSdk use this callback structure.
// It's deeply integrated in the JNI side too, so don't get fancy.
interface IResultListener<T> {
    fun onError(errorCode: Int, message: String)

    fun onStart()

    fun onSuccess(success: T)
}
