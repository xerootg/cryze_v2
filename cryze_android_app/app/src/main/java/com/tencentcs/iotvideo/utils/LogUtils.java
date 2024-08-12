package com.tencentcs.iotvideo.utils;

import android.os.Build;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import com.tencentcs.iotvideo.mars.xlog.Xlog;
import com.tencentcs.iotvideo.BuildConfig;
/* loaded from: classes2.dex */
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

    public static String composeLogInfo(Object... objArr) {
        if (objArr == null) {
            return "";
        }
        StringBuilder sb2 = new StringBuilder();
        for (Object obj : objArr) {
            sb2.append(obj);
        }
        return sb2.toString();
    }

    public static void d(String str, String str2) {
        d(str, str2, null);
    }

    public static void e(String str, String str2) {
        e(str, str2, null);
    }

    public static void f(String str, String str2) {
        f(str, str2, null);
    }

    public static void flush(boolean z10) {

    }

    public static int getLogLevel() {

        return 6;
    }

    public static String getSysInfo() {
        return SYS_INFO;
    }

    public static void i(String str, String str2) {
        i(str, str2, null);
    }


    public static void v(String str, String str2) {
        v(str, str2, null);
    }

    public static void w(String str, String str2) {
        w(str, str2, null);
    }

    public static void d(String str, String str2, Object... objArr) {
            if (objArr != null) {
                str2 = String.format(str2, objArr);
            }
            if (str2 == null) {
                str2 = "";
            }
            Log.d(str, str2);
    }

    public static void e(String str, String str2, Object... objArr) {
            if (objArr != null) {
                str2 = String.format(str2, objArr);
            }
            if (str2 == null) {
                str2 = "";
            }
            Log.e(str, str2);
    }

    public static void f(String str, String str2, Object... objArr) {
        if (logImp != null) {
            if (objArr != null) {
                str2 = String.format(str2, objArr);
            }
//            logImp.logF(str, "", "", 0, Process.myPid(), Process.myTid(), Looper.getMainLooper().getThread().getId(), str2);
            Log.d(str, str2);
        }
    }

    public static void i(String str, String str2, Object... objArr) {
            if (objArr != null) {
                str2 = String.format(str2, objArr);
            }
            if (str2 == null) {
                str2 = "";
            }
            Log.i(str, str2);
    }

    public static void v(String str, String str2, Object... objArr) {
            if (objArr != null) {
                str2 = String.format(str2, objArr);
            }
            if (str2 == null) {
                str2 = "";
            }
            Log.v(str, str2);
    }

    public static void w(String str, String str2, Object... objArr) {
            if (objArr != null) {
                str2 = String.format(str2, objArr);
            }
            if (str2 == null) {
                str2 = "";
            }
            Log.w(str, str2);

    }
}
