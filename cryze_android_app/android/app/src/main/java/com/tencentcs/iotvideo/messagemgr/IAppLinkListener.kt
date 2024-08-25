package com.tencentcs.iotvideo.messagemgr

import com.tencentcs.iotvideo.AppLinkState

interface IAppLinkListener {
    fun onAppLinkStateChanged(state: AppLinkState?)
}
