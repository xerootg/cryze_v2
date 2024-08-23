package com.tencentcs.iotvideo.messageparsers

data class TfInfoStVal(
    val capacity: Long,
    val free: Long,
    val status: Int,
    val recordStatus: Int
)

data class ProReadonlyTfInfo(
    val t: Long,
    val stVal: TfInfoStVal
)
