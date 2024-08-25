package com.tencentcs.iotvideo.utils;

import android.os.Build;
import android.util.Log;

// There's JNI glue that I haven't quite fully RE'd out yet.
public class LogUtils {
    private static final int LEVEL_DEBUG = 1;
    private static final int LEVEL_ERROR = 4;
    private static final int LEVEL_FATAL = 5;
    private static final int LEVEL_INFO = 2;
    private static final int LEVEL_NONE = 6;
    private static final int LEVEL_VERBOSE = 0;
    private static final int LEVEL_WARNING = 3;
    public static final String LOG_PREFIX = "IoTVideo-";
    private static final long MAX_ALIVE_TIME = 172800;
    private static final String SYS_INFO;
    private static boolean isOpenConsoleLog = false;
    private static boolean isOutLogImpl = false;
    private static int level = 0;
    private static Log logImp = null;
    private static Log mOrgDebugLogger = null;
    private static String sCacheLogPath = null;
    private static String sFilePrefix = "IoTVideo";
    private static String sLogPath;

    static {
        StringBuilder sb2 = new StringBuilder();
        try {
            sb2.append("VERSION.RELEASE:[" + Build.VERSION.RELEASE);
            sb2.append("] VERSION.CODENAME:[" + Build.VERSION.CODENAME);
            sb2.append("] VERSION.INCREMENTAL:[" + Build.VERSION.INCREMENTAL);
            sb2.append("] BOARD:[" + Build.BOARD);
            sb2.append("] DEVICE:[" + Build.DEVICE);
            sb2.append("] DISPLAY:[" + Build.DISPLAY);
            sb2.append("] FINGERPRINT:[" + Build.FINGERPRINT);
            sb2.append("] HOST:[" + Build.HOST);
            sb2.append("] MANUFACTURER:[" + Build.MANUFACTURER);
            sb2.append("] MODEL:[" + Build.MODEL);
            sb2.append("] PRODUCT:[" + Build.PRODUCT);
            sb2.append("] TAGS:[" + Build.TAGS);
            sb2.append("] TYPE:[" + Build.TYPE);
            sb2.append("] USER:[" + Build.USER + "]");
        } catch (Throwable th2) {
            th2.printStackTrace();
        }
        SYS_INFO = sb2.toString();
    }

    public static void close() {

    }

    public static String composeLogInfo(Object... logInfo) {
        if (logInfo == null) {
            return "";
        }
        StringBuilder sb2 = new StringBuilder();
        for (Object obj : logInfo) {
            sb2.append(obj);
        }
        return sb2.toString();
    }

    public static void d(String tag, String message) {
        d(tag, message, (Object[]) null);
    }

    public static void e(String tag, String message) {
        e(tag, message, (Object[]) null);
    }

    public static void f(String tag, String message) {
        f(tag, message, (Object[]) null);
    }

    public static void flush(boolean z10) {

    }

    public static int getLogLevel() {

        return LEVEL_NONE;
    }

    public static String getSysInfo() {
        return SYS_INFO;
    }

    public static void i(String tag, String message) {
        i(tag, message, (Object[]) null);
    }


    public static void v(String tag, String message) {
        v(tag, message, (Object[]) null);
    }

    public static void w(String tag, String message) {
        w(tag, message,(Object[]) null);
    }

    public static void d(String tag, String message, Object... stringArgs) {
            if (stringArgs != null) {
                message = String.format(message, stringArgs);
            }
            if (message == null) {
                message = "";
            }
            Log.d(tag, message);
    }

    public static void e(String tag, String message, Object... stringArgs) {
            if (stringArgs != null) {
                message = String.format(message, stringArgs);
            }
            if (message == null) {
                message = "";
            }
            Log.e(tag, message);
    }

    public static void f(String tag, String message, Object... stringArgs) {
        if (logImp != null) {
            if (stringArgs != null) {
                message = String.format(message, stringArgs);
            }
            Log.d(tag, message);
        }
    }

    public static void i(String tag, String message, Object... stringArgs) {
            if (stringArgs != null) {
                message = String.format(message, stringArgs);
            }
            if (message == null) {
                message = "";
            }
            Log.i(tag, message);
    }

    public static void v(String tag, String message, Object... stringArgs) {
            if (stringArgs != null) {
                message = String.format(message, stringArgs);
            }
            if (message == null) {
                message = "";
            }
            Log.v(tag, message);
    }

    public static void w(String tag, String message, Object... stringArgs) {
            if (stringArgs != null) {
                message = String.format(message, stringArgs);
            }
            if (message == null) {
                message = "";
            }
            Log.w(tag, message);
    }
}
