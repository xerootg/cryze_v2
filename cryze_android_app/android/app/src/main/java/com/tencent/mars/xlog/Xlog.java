package com.tencent.mars.xlog;

// This is used by JNI (libmarsxlog) to log. The structure must remain unchanged.
// Source (with mild changes for JNI complaints): https://github.com/Tencent/mars/blob/master/mars/libraries/mars_android_sdk/src/main/java/com/tencent/mars/xlog/Xlog.java
@SuppressWarnings("unused")
public class Xlog implements ILogger {
    // Let libiotvideo load marsxlog, there's an undeclared dependency that this cannot resolve.
    //    static {
    //        System.loadLibrary("c++_shared");
    //        System.loadLibrary("marsxlog");
    //    }
    public static final int AppednerModeAsync = 0;
    public static final int AppednerModeSync = 1;
    public static final int ZLIB_MODE = 0;
    public static final int ZSTD_MODE = 1;

    public static native void logWrite(XLoggerInfo xLoggerInfo, String str);

    public static native void logWrite2(int level, String tag, String filename, String funcName, int line, int pid, long tid, long mainTid, String log);

    @Override
    public native int getLogLevel();

    public static void setLogLevel(LogLevel level) {
        setLogLevel(level.toXlogLevel());
    }

    private static native void setLogLevel(int logLevel);

    public static native void setAppenderMode(int appenderMode);

    public static native void setConsoleLogOpen(boolean isConsoleLogOpen);

    public static native void setErrLogOpen(boolean isErrLogOpen);

    public static native void appenderOpen(int level, int mode, String cacheDir, String logDir, String namePrefix, int compressionMode, String pubKey);

    public static native void setMaxFileSize(long maxFileSize);

    public static native void setMaxAliveTime(long maxAliveTime);

    @Override
    public native void appenderClose();

    @Override
    public native void appenderFlush(boolean isSync);

    public static class XLoggerInfo {
        public int level;
        public String tag;
        public String filename;
        public String funcname;
        public int line;
        public long pid;
        public long tid;
        public long maintid;

        XLoggerInfo() {
        }
    }

    public static void open(LogLevel level, int mode, String cacheDir, String logDir, String namePrefix, String pubKey)
    {
        open(level.toXlogLevel(), mode, cacheDir, logDir, namePrefix, pubKey);
    }

    // Use unified log level enum for consistency
    private static void open(int level, int mode, String cacheDir, String logDir, String namePrefix, String pubKey) {
        appenderOpen(level, mode, cacheDir, logDir, namePrefix, ZLIB_MODE, pubKey);
    }

    private static String decryptTag(String tag) {
        return "IoTVideo-" + tag;
    }

    @Override
    public void logV(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(0, decryptTag(tag), filename, funcname, line, pid, tid, maintid, log);
    }

    @Override
    public void logD(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(LogLevel.LEVEL_DEBUG.toXlogLevel(), decryptTag(tag), filename, funcname, line, pid, tid, maintid, log);
    }

    @Override
    public void logI(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(LogLevel.LEVEL_INFO.toXlogLevel(), decryptTag(tag), filename, funcname, line, pid, tid, maintid, log);
    }

    @Override
    public void logW(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(LogLevel.LEVEL_WARNING.toXlogLevel(), decryptTag(tag), filename, funcname, line, pid, tid, maintid, log);
    }

    @Override
    public void logE(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(LogLevel.LEVEL_ERROR.toXlogLevel(), decryptTag(tag), filename, funcname, line, pid, tid, maintid, log);
    }

    @Override
    public void logF(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(LogLevel.LEVEL_FATAL.toXlogLevel(), decryptTag(tag), filename, funcname, line, pid, tid, maintid, log);
    }
}