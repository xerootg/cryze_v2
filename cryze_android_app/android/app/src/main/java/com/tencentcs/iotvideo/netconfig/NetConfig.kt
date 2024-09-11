package com.tencentcs.iotvideo.netconfig

import android.text.TextUtils
import com.google.gson.Gson
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.messagemgr.EventMessage
import com.tencentcs.iotvideo.messagemgr.IEventListener
import com.tencentcs.iotvideo.messagemgr.MessageMgr
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.rxjava.IResultListener

object NetConfig : INetConfig, IEventListener {

    // token - from Wyze
    // tid - a Wyze device ID
    // accessId - a Long from Wyze representing the account
    private external fun nativeSubscribeDevice(token: String, tid: String): Int

    // TODO: Determine if this is discarding events we need to be more resiliant
    override fun onNotify(eventMessage: EventMessage?) {
        if (eventMessage != null) {
            if (DEVICE_ONLINE_TOPIC == eventMessage.topic) {
                val netConfigResult =
                    Gson().fromJson(eventMessage.data, NetConfigResult::class.java)
                LogUtils.i(TAG, "onNotify:$netConfigResult")
            } else {
                LogUtils.w(TAG, "onNotify: ${eventMessage.topic} ${eventMessage.data}")
            }
        }
    }

    // JNI seems to want this to exist. it does not and cannot do anything with this function,
    // it doesn't have the references needed.
    @SuppressWarnings("unused")
    private fun subscribeDevice(
        token: String,
        tid: String,
        iResultListener: IResultListener<Boolean>
    ) {
        trySubscribeDevice(token, tid, iResultListener, false)
    }

    // This gets a backend ID for the device, and then adds the device to MessageMgr's map of devices.
    // The onSubscribeMessage function is called by JNI
    @Synchronized
    // nativeSubscribeDevice can only handle one subscription at a time, so we need to synchronize
    override fun trySubscribeDevice(
        token: String,
        tid: String,
        iResultListener: IResultListener<Boolean>,
        force: Boolean
    ): Boolean {
        // first thing, check if we are already subscribed to this device
        if (MessageMgr.deviceSubscribed(iResultListener)) {
            LogUtils.i(TAG, "subscribeDevice already subscribed")
            if (!force) {
                return true
            }
            // remove the old subscription
            MessageMgr.removeSubscribeDevice(iResultListener)
            LogUtils.i(TAG, "subscribeDevice removed old subscription")
        }

        LogUtils.i(TAG, "subscribeDevice tid = $tid token = $token")
        if (TextUtils.isEmpty(token)) {
            iResultListener.onError(-1, "devToken is null")
            return false
        }
        val nativeSubscribeDevice = nativeSubscribeDevice(token, tid)
        LogUtils.i(TAG, "subscribeDevice msgId = $nativeSubscribeDevice")
        if (nativeSubscribeDevice < 0) {
            iResultListener.onError(-1, "subscribe device is error:$nativeSubscribeDevice")
            return false
        }

        MessageMgr.addSubscribeDevice(nativeSubscribeDevice, iResultListener)
        return true
    }

    private const val DEVICE_ONLINE_TOPIC = "\$Device.DevBindChkResult"
    private const val TAG = "NetConfig"
}
