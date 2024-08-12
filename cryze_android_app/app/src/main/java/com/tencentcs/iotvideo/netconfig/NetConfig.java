package com.tencentcs.iotvideo.netconfig;

import android.text.TextUtils;
import android.util.Log;

import com.tencentcs.iotvideo.IoTVideoSdk;
import com.tencentcs.iotvideo.netconfig.data.NetMatchTokenResult;
import com.tencentcs.iotvideo.utils.LogUtils;
import com.tencentcs.iotvideo.utils.rxjava.IResultListener;

import java.util.concurrent.ConcurrentHashMap;

public class NetConfig implements INetConfig {
    private static final String DEVICE_ONLINE_TOPIC = "$Device.DevBindChkResult";
    private static final String ERROR_TOKEN_IS_NULL = "error_token_is_null";
    private static final int ERROR_TOKEN_RESPONSE_NULL = 2021;
    private static final int MAX_WAIT_DEVICE_ONLINE_TIME = 300;
    private static final int QUERY_TIME_INTERVAL = 5;
    private static final String TAG = "NetConfig";

    private ConcurrentHashMap<Integer, IResultListener<Boolean>> mMsgIdMap;

    private static native int nativeSubscribeDevice(String str, String str2);

    public static class NetConfigHolder {
        private static final NetConfig INSTANCE = new NetConfig();

        private NetConfigHolder() {
        }
    }


    public static NetConfig getInstance() {
        return NetConfigHolder.INSTANCE;
    }

    @Override
    public void getNetConfigToken(String str, String str2, int i10, IResultListener<NetMatchTokenResult> iResultListener) {

    }

    @Override
    public void getNetConfigToken(String str, String str2, IResultListener<NetMatchTokenResult> iResultListener) {

    }

    @Override
    public void intervalQueryDeviceOnlineStatus() {

    }

    @Override
    public void queryDeviceOnlineStatus(IOnlineStatusListener iOnlineStatusListener) {

    }

    @Override
    public void registerDeviceOnlineCallback(INetConfigResultListener iNetConfigResultListener) {

    }

    @Override
    public int subscribeDevice(String token, String deviceId) {
        LogUtils.i(TAG, "subscribeDevice tid = " + deviceId + " toke = " + token);
        if (token.isEmpty())
        {
            LogUtils.e(TAG, "subscribeDevice result = -1 token is empty");
            return -1;
        }
        int nativeSubscribeDevice = nativeSubscribeDevice(token, deviceId);
        LogUtils.i(TAG, "subscribeDevice result = " + nativeSubscribeDevice);
        return nativeSubscribeDevice;
    }

    @Override
    public void subscribeDevice(String str, String str2, IResultListener<Boolean> iResultListener) {
        LogUtils.i(TAG, "subscribeDevice tid = " + str2 + " token = " + str);
        if (TextUtils.isEmpty(str)) {
            if (iResultListener != null) {
                iResultListener.onError(-1, "devToken is null");
            }
            return;
        }
        int nativeSubscribeDevice = nativeSubscribeDevice(str, str2);
        LogUtils.i(TAG, "subscribeDevice msgId = " + nativeSubscribeDevice);
        if (nativeSubscribeDevice < 0) {
            if (iResultListener != null) {
                iResultListener.onError(-1, "subscribe device is error:" + nativeSubscribeDevice);
                return;
            }
            return;
        }
        if (this.mMsgIdMap == null) {
            this.mMsgIdMap = new ConcurrentHashMap<>();
        }
        if (iResultListener != null) {
            this.mMsgIdMap.put(nativeSubscribeDevice, iResultListener);
            IoTVideoSdk.getMessageMgr().subscribeDevice(this.mMsgIdMap);
        }
    }

    @Override
    public void unregisterDeviceOnlineCallback(INetConfigResultListener iNetConfigResultListener) {

    }
}
