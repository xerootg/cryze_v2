package com.tencentcs.iotvideo.iotvideoplayer

//TODO: This is intriguing, it deals with jitter.
// i10 is fps
// i11 i *think* is expected fps
// i12 is ... needs investigation
interface IVideoFluctuationStrategy {
    fun calculateStrategy(i10: Int, i11: Int, i12: Int): Int

    fun resetStrategy()
}
