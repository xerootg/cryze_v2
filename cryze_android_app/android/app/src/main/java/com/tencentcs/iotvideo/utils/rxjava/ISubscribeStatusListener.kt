package com.tencentcs.iotvideo.utils.rxjava


interface ISubscribeStatusListener<T> : IResultListener<T> {
    // The JNI expects IResultListener to implement
    // IResultListener but doesn't actually use onStart
    override fun onStart() {
    }
}