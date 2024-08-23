package com.tencentcs.iotvideo

import java.io.PrintWriter
import java.io.StringWriter
import com.tencentcs.iotvideo.utils.LogUtils

object StackTraceUtils {

    // I don't want this object in the stacktrace, so I'm inlining it.
    @Suppress("NOTHING_TO_INLINE")
    @JvmStatic
    inline fun logStackTrace(TAG: String, reason: String){
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        Throwable().printStackTrace(pw)
        val stackTrace = sw.toString()

        LogUtils.e(TAG , "$reason: stackTrace: $stackTrace")
    }
}