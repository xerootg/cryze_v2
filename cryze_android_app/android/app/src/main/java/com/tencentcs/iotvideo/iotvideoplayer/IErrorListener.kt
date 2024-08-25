package com.tencentcs.iotvideo.iotvideoplayer

import com.tencentcs.iotvideo.messagemgr.SubscribeError

//TODO: use the correct Enum, I think we have it
interface IErrorListener {
    fun onError(errorCode: Int){
        onError(SubscribeError.fromErrorCode(errorCode))
    }
    fun onError(errorType: SubscribeError)
}
