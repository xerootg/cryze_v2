package com.tencentcs.iotvideo.netconfig;

import com.tencentcs.iotvideo.netconfig.data.NetMatchTokenResult;
import com.tencentcs.iotvideo.utils.rxjava.IResultListener;
/* loaded from: classes2.dex */
public interface INetConfig {
    void getNetConfigToken(String str, String str2, int i10, IResultListener<NetMatchTokenResult> iResultListener);

    void getNetConfigToken(String str, String str2, IResultListener<NetMatchTokenResult> iResultListener);

    void intervalQueryDeviceOnlineStatus();

    void queryDeviceOnlineStatus(IOnlineStatusListener iOnlineStatusListener);

    void registerDeviceOnlineCallback(INetConfigResultListener iNetConfigResultListener);

    int subscribeDevice(String token, String deviceId);

    void subscribeDevice(String str, String str2, IResultListener<Boolean> iResultListener);

    void unregisterDeviceOnlineCallback(INetConfigResultListener iNetConfigResultListener);
}