package com.tencent.mars.xlog;

/* loaded from: iotvideo-release.aar:classes.jar:com/tencent/mars/xlog/ILogger.class */
public interface ILogger {
    void logV(String str, String str2, String str3, int i, int i2, long j, long j2, String str4);

    void logI(String str, String str2, String str3, int i, int i2, long j, long j2, String str4);

    void logD(String str, String str2, String str3, int i, int i2, long j, long j2, String str4);

    void logW(String str, String str2, String str3, int i, int i2, long j, long j2, String str4);

    void logE(String str, String str2, String str3, int i, int i2, long j, long j2, String str4);

    void logF(String str, String str2, String str3, int i, int i2, long j, long j2, String str4);

    int getLogLevel();

    void appenderClose();

    void appenderFlush(boolean z);
}