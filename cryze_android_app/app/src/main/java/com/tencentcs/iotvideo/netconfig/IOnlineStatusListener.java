package com.tencentcs.iotvideo.netconfig;

public interface IOnlineStatusListener {
    void onFail(int i10, String str);

    void onNetConfigResult(NetConfigResult netConfigResult);
}

