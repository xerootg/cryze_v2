package com.tencent.mars.xlog;

public class Xlog {

    public static final int LEVEL_ALL = 0;
    public static final int LEVEL_VERBOSE = 0;
    public static final int LEVEL_DEBUG = 1;
    public static final int LEVEL_INFO = 2;
    public static final int LEVEL_WARNING = 3;
    public static final int LEVEL_ERROR = 4;
    public static final int LEVEL_FATAL = 5;
    public static final int LEVEL_NONE = 6;

    public static final int COMPRESS_LEVEL1 = 1;
    public static final int COMPRESS_LEVEL2 = 2;
    public static final int COMPRESS_LEVEL3 = 3;
    public static final int COMPRESS_LEVEL4 = 4;
    public static final int COMPRESS_LEVEL5 = 5;
    public static final int COMPRESS_LEVEL6 = 6;
    public static final int COMPRESS_LEVEL7 = 7;
    public static final int COMPRESS_LEVEL8 = 8;
    public static final int COMPRESS_LEVEL9 = 9;

    public static final int AppednerModeAsync = 0;
    public static final int AppednerModeSync = 1;

    public static final int ZLIB_MODE = 0;
    public static final int ZSTD_MODE = 1;

    static class XLoggerInfo {
        public int level;
        public String tag;
        public String filename;
        public String funcname;
        public int line;
        public long pid;
        public long tid;
        public long maintid;
    }

    public static class XLogConfig {
        public int level = LEVEL_INFO;
        public int mode = AppednerModeAsync;
        public String logdir;
        public String nameprefix;
        public String pubkey = "";
        public int compressmode = ZLIB_MODE;
        public int compresslevel = 0;
        public String cachedir;
        public int cachedays = 0;
    }

    public static void open(int level, int mode, String cacheDir, String logDir, String namePrefix, String pubKey) {
        XLogConfig config = new XLogConfig();
            config.level = level;
            config.mode = mode;
            config.logdir = logDir;
            config.nameprefix = namePrefix;
            config.compressmode = ZLIB_MODE;
            config.pubkey = pubKey;
            config.cachedir = cacheDir;
            config.cachedays = 0;
        appenderOpen(config);
    }

    public static native void setConsoleLogOpen(boolean z10);

    public static native void setLogLevel(int i10);

    public static native void setMaxAliveTime(long j10);

    private static String decryptTag(String tag) {
        return tag;
    }

    
    public void logV(long logInstancePtr, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(logInstancePtr, LEVEL_VERBOSE, decryptTag(tag), filename, funcname, line, pid, tid, maintid, log);
    }

    
    public void logD(long logInstancePtr, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(logInstancePtr, LEVEL_DEBUG, decryptTag(tag), filename, funcname, line, pid, tid, maintid, log);
    }

    
    public void logI(long logInstancePtr, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(logInstancePtr, LEVEL_INFO, decryptTag(tag), filename, funcname, line, pid, tid, maintid,  log);
    }

    
    public void logW(long logInstancePtr, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(logInstancePtr, LEVEL_WARNING, decryptTag(tag), filename, funcname, line, pid, tid, maintid,  log);
    }

    
    public void logE(long logInstancePtr, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(logInstancePtr, LEVEL_ERROR, decryptTag(tag), filename, funcname, line, pid, tid, maintid,  log);
    }

    
    public void logF(long logInstancePtr, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        logWrite2(logInstancePtr, LEVEL_FATAL, decryptTag(tag), filename, funcname, line, pid, tid, maintid, log);
    }


    
    public void appenderOpen(int level, int mode, String cacheDir, String logDir, String nameprefix, int cacheDays) {

        XLogConfig logConfig = new XLogConfig();
        logConfig.level = level;
        logConfig.mode = mode;
        logConfig.logdir = logDir;
        logConfig.nameprefix = nameprefix;
        logConfig.compressmode = ZLIB_MODE;
        logConfig.pubkey = "";
        logConfig.cachedir = cacheDir;
        logConfig.cachedays = cacheDays;

        appenderOpen(logConfig);
    }

    public static native void logWrite(XLoggerInfo logInfo, String log);

    public static void logWrite2(int level, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log){
        logWrite2(0, level, tag, filename ,funcname, line, pid, tid, maintid, log);
    }

    public static native void logWrite2(long logInstancePtr, int level, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log);

    
    public native int getLogLevel(long logInstancePtr);


    
    public native void setAppenderMode(long logInstancePtr, int mode);

    
    public long openLogInstance(int level, int mode, String cacheDir, String logDir, String nameprefix, int cacheDays) {
        XLogConfig logConfig = new XLogConfig();
        logConfig.level = level;
        logConfig.mode = mode;
        logConfig.logdir = logDir;
        logConfig.nameprefix = nameprefix;
        logConfig.compressmode = ZLIB_MODE;
        logConfig.pubkey = "";
        logConfig.cachedir = cacheDir;
        logConfig.cachedays = cacheDays;
        return newXlogInstance(logConfig);
    }

    
    public native long getXlogInstance(String nameprefix);

    
    public native void releaseXlogInstance(String nameprefix);

    public native long newXlogInstance(XLogConfig logConfig);

    
    public native void setConsoleLogOpen(long logInstancePtr, boolean isOpen);	//set whether the console prints log

    private static native Long appenderOpen(XLogConfig logConfig);

    
    public native void appenderClose();

    
    public native void appenderFlush(long logInstancePtr, boolean isSync);

    
    public native void setMaxFileSize(long logInstancePtr, long aliveSeconds);

    
    public native void setMaxAliveTime(long logInstancePtr, long aliveSeconds);

}