package com.tencentcs.iotvideo.netconfig;

import com.tencentcs.iotvideo.utils.rxjava.IResultListener;

public interface INetConfig {

    void subscribeDevice(String str, String str2, IResultListener<Boolean> iResultListener);
}