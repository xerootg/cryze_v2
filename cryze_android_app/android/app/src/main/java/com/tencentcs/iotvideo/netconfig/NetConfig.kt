package com.tencentcs.iotvideo.netconfig

import android.text.TextUtils
import com.google.gson.Gson
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.messagemgr.EventMessage
import com.tencentcs.iotvideo.messagemgr.IEventListener
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.rxjava.IResultListener
import java.util.concurrent.ConcurrentHashMap

class NetConfig private constructor() : INetConfig, IEventListener {
    private var mMsgIdMap: ConcurrentHashMap<Int, IResultListener<Boolean>>? = null

    private external fun nativeSubscribeDevice(str: String, str2: String): Int

    override fun onNotify(eventMessage: EventMessage) {
        if (DEVICE_ONLINE_TOPIC == eventMessage.topic) {
            val netConfigResult = Gson().fromJson(eventMessage.data, NetConfigResult::class.java)
            LogUtils.i(TAG, "onNotify:$netConfigResult")
        }
    }

    override fun subscribeDevice(
        token: String,
        tid: String,
        iResultListener: IResultListener<Boolean>
    ) {
        LogUtils.i(TAG, "subscribeDevice tid = $tid token = $token")
        if (TextUtils.isEmpty(token)) {
            iResultListener.onError(-1, "devToken is null")
            return
        }
        val nativeSubscribeDevice = nativeSubscribeDevice(token, tid)
        LogUtils.i(TAG, "subscribeDevice msgId = $nativeSubscribeDevice")
        if (nativeSubscribeDevice < 0) {
            iResultListener.onError(-1, "subscribe device is error:$nativeSubscribeDevice")
            return
        }
        if (this.mMsgIdMap == null) {
            this.mMsgIdMap = ConcurrentHashMap()
        }
        mMsgIdMap!![nativeSubscribeDevice] = iResultListener
        IoTVideoSdk.getMessageMgr().subscribeDevice(this.mMsgIdMap)
    }

    companion object {
        val instance: NetConfig = NetConfig()

        private const val DEVICE_ONLINE_TOPIC = "\$Device.DevBindChkResult"
        private const val TAG = "NetConfig"
    }
}
