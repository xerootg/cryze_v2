package com.tencentcs.iotvideo.messagemgr

interface IEventListener {
    fun onNotify(eventMessage: EventMessage?)
}
