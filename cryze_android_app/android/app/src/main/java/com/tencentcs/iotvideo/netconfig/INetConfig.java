package com.tencentcs.iotvideo.netconfig;

import com.tencentcs.iotvideo.utils.rxjava.IResultListener;

public interface INetConfig {

    boolean trySubscribeDevice(String token, String tid, IResultListener<Boolean> iResultListener, Boolean force);
}