package com.tencentcs.iotvideo.iotvideoplayer

/* loaded from: classes2.dex */
interface IVideoStuckStrategy {
    fun isVideoStuck(i10: Int, i11: Int): Boolean

    fun resetStrategy()
}
