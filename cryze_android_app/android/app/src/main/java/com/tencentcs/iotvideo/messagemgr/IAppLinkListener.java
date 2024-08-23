package com.tencentcs.iotvideo.messagemgr;

import com.tencentcs.iotvideo.AppLinkState;

public interface IAppLinkListener {
    void onAppLinkStateChanged(AppLinkState state);
}
