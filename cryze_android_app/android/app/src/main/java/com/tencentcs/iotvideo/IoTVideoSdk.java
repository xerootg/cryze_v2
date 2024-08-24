// Original file: IoTVideoSdk.java
package com.tencentcs.iotvideo;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import android.text.TextUtils;
import android.util.Log;

import com.tencent.mars.xlog.Xlog;
import com.tencentcs.iotvideo.messagemgr.IMessageMgr;
import com.tencentcs.iotvideo.messagemgr.MessageMgr;
import com.tencentcs.iotvideo.netconfig.INetConfig;
import com.tencentcs.iotvideo.netconfig.Language;
import com.tencentcs.iotvideo.netconfig.NetConfig;
import com.tencentcs.iotvideo.utils.LogUtils;
import com.tencentcs.iotvideo.utils.Utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IoTVideoSdk {

    public static void unRegister() {
        if(registerState != SDK_REGISTER_STATE_UNREGISTERED) {
            unRegister(false);
        }
    }

    public static class NetWorkStateReceiver extends BroadcastReceiver {
        private NetWorkStateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                LogUtils.i(IoTVideoSdk.TAG, "Connectivity Change");
                if (!IoTVideoSdk.isSupportedCurrentAbi()) {
                    LogUtils.e(IoTVideoSdk.TAG, "onReceive don't support abi");
                } else if (MessageMgr.getSdkStatus() != AppLinkState.APP_LINK_ONLINE) {
                    LogUtils.i(IoTVideoSdk.TAG, "app is not online");
                } else {
                    ExecutorService newSingleThreadExecutor = Executors.newSingleThreadExecutor();
                    newSingleThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (3 != IoTVideoSdk.registerState) {
                                IoTVideoSdk.onNetworkChanged();
                            } else {
                                LogUtils.e(IoTVideoSdk.TAG, "onReceive failure");
                            }
                        }
                    });
                    newSingleThreadExecutor.shutdown();
                }
            }
        }
    }

    public static final String IOT_HOST = "|wyze-mars-asrv.wyzecam.com";
    private static NetWorkStateReceiver mNetWorkStateReceiver;

    public static final int APP_LINK_ACCESS_TOKEN_ERROR = 3;
    public static final int APP_LINK_DEV_DISABLE = 7;
    public static final int APP_LINK_INVALID_TID = 5;
    public static final int APP_LINK_KICK_OFF = 6;
    public static final int APP_LINK_OFFLINE = 2;
    public static final int APP_LINK_ONLINE = 1;
    public static final int APP_LINK_TID_INIT_ERROR = 4;
    public static final int DEV_TYPE_DID = 2;
    public static final int DEV_TYPE_THIRD_ID = 3;
    public static final int DEV_TYPE_TID = 1;
    public static final int LAN_DEV_CONNECTABLE_STATE_COULD_NOT_USE = 0;
    public static final int LAN_DEV_CONNECTABLE_STATE_COULD_USE = 1;
    public static final int LAN_DEV_CONNECTABLE_STATE_NOT_INIT = -1;
    public static final int LAN_DEV_CONNECTABLE_STATE_PARAMS_INVALID = -3;
    public static final int LAN_DEV_CONNECTABLE_STATE_USER_TYPE_INVALID = -2;
    public static final int LOG_LEVEL_DEBUG = 5;
    public static final int LOG_LEVEL_ERROR = 2;
    public static final int LOG_LEVEL_FATAL = 1;
    public static final int LOG_LEVEL_INFO = 4;
    public static final int LOG_LEVEL_OFF = 0;
    public static final int LOG_LEVEL_VERBOSE = 6;
    public static final int LOG_LEVEL_WARNING = 3;
    public static final String PREFIX_THIRD_ID = "_@.";
    public static final int SDK_REGISTER_STATE_IDL = 0;
    public static final int SDK_REGISTER_STATE_REGISTERED = 2;
    public static final int SDK_REGISTER_STATE_REGISTERING = 1;
    public static final int SDK_REGISTER_STATE_UNREGISTERED = 4;
    public static final int SDK_REGISTER_STATE_UNREGISTERING = 3;
    private static final String TAG = "CLIoTVideoSdk";
    private static final long VALID_REGISTER_TIME_INTERVAL = 1000;
    private static Language lastRegisterAppSysLanguage;
    private static int lastRegisterDeviceType;
    private static Application mContext;
    private static Lock mReceiverLock;
    private static int registerState;
    private static String[] supportedAbi;
    private static HashMap<String, Object> sUserInfo = new HashMap<>();
    private static long lastRegisterTime = 0;
    private static long lastRegisterAccessId = -1;
    private static String lastRegisterAccessToken = "";
    private static boolean isWaitingUnregister = false;
    private static boolean isInited = false;

    public static int getRegisterState() {
        return registerState;
    }

    static {
        if (isSupportedCurrentAbi()) {
            try {
                System.loadLibrary("mbedtls");
                System.loadLibrary("iotp2pav");
                System.loadLibrary("iotvideo");
            } catch (InternalError err) {
                Log.e(TAG, "iot video NOT sdk statically created! - could not load libraries");
            }
            Log.i(TAG, "iot video sdk statically created!");
        } else {
            Log.e(TAG, "iot video NOT sdk statically created!");
        }
        mReceiverLock = new ReentrantLock();
    }

    public static void init(Application application, HashMap<String, Object> options) {
        boolean isSupportedCurrentAbi = isSupportedCurrentAbi();
        supportedAbi = Utils.getSupportedAbi();
        LogUtils.i(TAG, "init version is 1.0.0; supported Abi: " + Arrays.toString(supportedAbi));
        isInited = isSupportedCurrentAbi;
        if (!isSupportedCurrentAbi) {
            return;
        }
        mContext = application;
        sUserInfo = options;
        if (options == null) {
            sUserInfo = new HashMap<>();
        }
        Log.i(TAG, "init successful");
    }

    public static IMessageMgr getMessageMgr() {
        return MessageMgr.getInstance();
    }

    public static INetConfig getNetConfig() {
        return NetConfig.getInstance();
    }

    private static void registerNetBroadcastReceiver() {
        mReceiverLock.lock();
        if (mNetWorkStateReceiver == null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            NetWorkStateReceiver netWorkStateReceiver = new NetWorkStateReceiver();
            mNetWorkStateReceiver = netWorkStateReceiver;
            mContext.registerReceiver(netWorkStateReceiver, intentFilter);
            LogUtils.i(TAG, "registerNetBroadcastReceiver");
        }
        mReceiverLock.unlock();
    }

    public static boolean isInited() {
        return isInited;
    }


    public static void register(long accessId, String accessToken) {
        register(accessId, accessToken, DEV_TYPE_TID);
    }

    public static void register(long accessId, String accessToken, int deviceType) {
        register(accessId, accessToken, deviceType, Language.NONE);
    }
    public static void register(long accessId, String accessToken, int deviceType, Language sysLanguage)
    {
        LogUtils.e(TAG, "IotVideoSdk Register Started");
        if (!isInited())
        {
            LogUtils.e(TAG, "register sdk error: sdk is not init");
        } else if (isSupportedCurrentAbi()) {
            LogUtils.i(TAG, "Starting IoTVideoSdk register!");
            lastRegisterAccessId = accessId;
            lastRegisterAccessToken = accessToken;
            lastRegisterDeviceType = deviceType;
            lastRegisterAppSysLanguage = sysLanguage;
            lastRegisterTime = System.currentTimeMillis();
            LogUtils.i(TAG, "register registerState:" + registerState);
            if (registerState != SDK_REGISTER_STATE_IDL && SDK_REGISTER_STATE_UNREGISTERED != registerState) //SDK_REGISTER_STATE_REGISTERED
            {
                LogUtils.i(TAG, "register,auto unregister");
                isWaitingUnregister = true;
                unRegister(true);
                return;
            }
            isWaitingUnregister = false;
            registerState = SDK_REGISTER_STATE_REGISTERING;
            MessageMgr.getInstance().register(mContext);
            String[] split = "1.0.0".split("\\(")[0].split("\\.");
            int version_code = 1 | (Integer.parseInt(split[1]) << 19) | (Integer.parseInt(split[0]) << 23) | 268435456;
            int p2p_port_type = (sUserInfo == null || sUserInfo.get("IOT_P2P_PORT_TYPE") == null) ? 0 : ((Integer) sUserInfo.get("IOT_P2P_PORT_TYPE")).intValue();
            LogUtils.i(TAG, "register p2p_port_type:" + p2p_port_type);
            try {
                LogUtils.i(TAG, "register test net:" + getLocalIPAddress());
            } catch (Exception e2) {
                LogUtils.e(TAG, "register error:" + e2.getMessage());
            }
            nativeRegister(accessId, accessToken, IOT_HOST, version_code, (short) deviceType, (short) sysLanguage.getLanguage(), p2p_port_type);
            LogUtils.i(TAG, "register accessId" + accessId + " accessToken: " + accessToken + ", version = 0x" + Integer.toHexString(version_code) + ", p2pUrl = " + IOT_HOST + "; language :" + sysLanguage.name());

            registerNetBroadcastReceiver();
            registerState = SDK_REGISTER_STATE_REGISTERED;
        }
    }


    private static void unRegister(boolean autoRegister) {
        LogUtils.i(TAG, "unregister, isAuto:" + autoRegister);
        if (isSupportedCurrentAbi()) {
            if (!isInited()) {
                LogUtils.e(TAG, "unregister error:sdk is not init");
                return;
            }
            if (!autoRegister) {
                lastRegisterTime = 0L;
                lastRegisterAccessId = -1L;
                lastRegisterAccessToken = null;
                lastRegisterDeviceType = 0;
                lastRegisterAppSysLanguage = Language.NONE;
                isWaitingUnregister = false;
            }
            registerState = 3;
            MessageMgr.getInstance().unregister(!autoRegister);
            ExecutorService newSingleThreadExecutor = Executors.newSingleThreadExecutor();
            newSingleThreadExecutor.execute(new Runnable() { // from class: com.tencentcs.iotvideo.IoTVideoSdk.1
                @Override // java.lang.Runnable
                public void run() {
                    IoTVideoSdk.nativeUnregister();
                    int unused = IoTVideoSdk.registerState = SDK_REGISTER_STATE_UNREGISTERED;
                    LogUtils.i(IoTVideoSdk.TAG, "unregister finish, isWaitingUnregister:" + IoTVideoSdk.isWaitingUnregister);
                    if (IoTVideoSdk.isWaitingUnregister) {
                        if (-1 != IoTVideoSdk.lastRegisterAccessId && !TextUtils.isEmpty(IoTVideoSdk.lastRegisterAccessToken)) {
                            LogUtils.i(IoTVideoSdk.TAG, "register after auto unregister");
                            long unused2 = IoTVideoSdk.lastRegisterTime = 0L;
                            IoTVideoSdk.register(IoTVideoSdk.lastRegisterAccessId, IoTVideoSdk.lastRegisterAccessToken, IoTVideoSdk.lastRegisterDeviceType, IoTVideoSdk.lastRegisterAppSysLanguage);
                            return;
                        }
                        LogUtils.e(IoTVideoSdk.TAG, "register after auto unregister error:input params is invalid");
                    }
                }
            });
            newSingleThreadExecutor.shutdown();
            unregisterNetBroadcastReceiver();
        }
    }

    private static void unregisterNetBroadcastReceiver() {
        mReceiverLock.lock();
        if (mNetWorkStateReceiver != null) {
            LogUtils.i(TAG, "unregisterNetBroadcastReceiver");
            mContext.unregisterReceiver(mNetWorkStateReceiver);
            mNetWorkStateReceiver = null;
        }
        mReceiverLock.unlock();
    }

    public static boolean isSupportedCurrentAbi() {
        String[] strArr;
        if (supportedAbi == null) {
            supportedAbi = Build.SUPPORTED_ABIS;
        }
        for (String str : supportedAbi) {
            if (TextUtils.equals(str, "arm64-v8a") || TextUtils.equals(str, "armeabi-v7a") || TextUtils.equals(str, "armeabi")) {
                return true;
            }
        }
        return false;
    }
    private static native long nGetDidFromTid(String str);
    private static native int nGetHLSHttpPort();
    public static int getHLSHttpPort()
    {
        return nGetHLSHttpPort();
    }
    public static int lanDevConnectable(String deviceId) {
               if (!isInited()) {
            return LAN_DEV_CONNECTABLE_STATE_NOT_INIT;
        }
        if (TextUtils.isEmpty(deviceId)) {
            LogUtils.e(TAG, "isLanDevConnectable error:input params is invalid");
            return LAN_DEV_CONNECTABLE_STATE_PARAMS_INVALID;
        }
        return nIsLanDevConnectable(deviceId);

    }

    static public native int setDebugMode(int val);
    static public native int setLogPath(String str);

    private static native int nIsLanDevConnectable(String str);
    private static native int nativeCheckAndSetDevicePwd(String str, String str2, String str3);
    private static native String[] nativeGetAnonymousSecureKey(String str, long j10);
    private static native int nativeGetP2PVersion();

    public static int getP2PVersion()
    {
        return nativeGetP2PVersion();
    }
    private static native byte[] nativeGetRstpPassword(String str);
    private static native long nativeGetTerminalId();
    private static native void nativeRegister(long accessId, String accessToken, String host, int version_code, short deviceType, short language, int p2pPortType);
    private static native String nativeSha1WithBase256(String str, String str2);
    private static native String nativeSha256WithHex(String str);
    private static native void nativeStartDetectDevNetwork(String str, String str2, int i10, int i11);
    public static native void nativeUnregister();
    private static native int nativeUpdateToken(String str);
    public static native void onNetworkChanged();
    private static native byte[] uncompressStr(byte[] bArr, int i10);

    public static String getLocalIPAddress() {
        Enumeration<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        if (networkInterfaces == null) {
            return "null";
        }
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface nextElement = networkInterfaces.nextElement();
            if (nextElement != null) {
                Enumeration<InetAddress> inetAddresses = nextElement.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress nextElement2 = inetAddresses.nextElement();
                    if (!nextElement2.isLoopbackAddress() && (nextElement2 instanceof Inet4Address)) {
                        return nextElement2.getHostAddress();
                    }
                }
                continue;
            }
        }
        return "null";
    }
}


