package com.tencentcs.iotvideo.messagemgr

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Looper
import com.tencentcs.iotvideo.AppLinkState
import com.tencentcs.iotvideo.AppLinkState.Companion.fromInt
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.StackTraceUtils.logStackTrace
import com.tencentcs.iotvideo.utils.FileIOUtils.readFile2BytesByStream
import com.tencentcs.iotvideo.utils.FileIOUtils.writeFileFromBytesByStream
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.rxjava.IResultListener
import java.nio.file.Paths
import java.util.Arrays
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min

object MessageMgr {
    private val mAppLinkListeners = CopyOnWriteArrayList<IAppLinkListener?>()
    private var mSubscribeDeviceMap: ConcurrentHashMap<Int, IResultListener<Boolean>> = ConcurrentHashMap()
    private val mModelListeners: MutableList<IModelListener?> = ArrayList()
    private val mEventListeners: MutableList<IEventListener?> = ArrayList()
    private val mMainHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun register(application: Application?) {
        LogUtils.i(TAG, "MessageMgr register")
        mContext = application
        nativeRegister()
    }

    // unused but required by JNI
    @Suppress("unused")
    private external fun nativeGetData(str: String?, str2: String?): Long

    @Suppress("unused")
    private external fun nativeNotifyDevModelToServer(str: String, str2: String)

    @Suppress("unused")
    private external fun nativeSendDataToDevice(
        str: String?,
        bArr: ByteArray?,
        i10: Int,
        i11: Int
    ): Long

    @Suppress("unused")
    private external fun nativeSendDataToServer(str: String?, bArr: ByteArray?): Long

    @Suppress("unused")
    private external fun nativeSetData(str: String?, str2: String?, str3: String?): Long

    @Suppress("unused")
    private external fun nativeSetUserParam(
        str: String?,
        str2: String?,
        str3: String?,
        str4: String?
    ): Long

    // used by this class
    private external fun nativeRegister()
    private external fun nativeUnregister()

    private fun dispatchEventMessage(eventMessage: EventMessage) {
        if (mEventListeners.isEmpty()) {
            LogUtils.i(TAG, "no event listeners to send to! status: $sdkStatus")
        } else {
            val evtMessageAction = Runnable {
                for (mEventListener in mEventListeners) {
                    mEventListener!!.onNotify(eventMessage)
                }
            }
            if (isMainThread) {
                evtMessageAction.run()
            } else {
                mMainHandler.post(evtMessageAction)
            }
        }
    }

    private fun dispatchModelMessage(modelMessage: ModelMessage?) {
        if (modelMessage == null) {
            LogUtils.e(TAG, "dispatchModelMessage modelMessage is null")
            return
        }
        if (mModelListeners.isEmpty()) {
            LogUtils.i(TAG, "no model listeners to send to! status: $sdkStatus")
        } else {
            if (isMainThread) {
                for (mModelListener in mModelListeners) {
                    mModelListener!!.onNotify(modelMessage)
                }
            } else {
                mMainHandler.post(Runnable {
                    for (mModelListener in mModelListeners) {
                        mModelListener?.onNotify(modelMessage)
                    }
                })
            }
        }
    }

    @Suppress("unused") // JNI
    fun unregister() {
        unregister(true)
    }


    @JvmStatic
    fun unregister(clearCacheData: Boolean) {
        LogUtils.i(TAG, "unregister start， clearCacheData：$clearCacheData")
        if (clearCacheData) {
            mContext = null
            mModelListeners.clear()
        }
        nativeUnregister()
        sdkStatus = AppLinkState.APP_LINK_OFFLINE
        LogUtils.i(TAG, "unregister end")
    }

    fun addAppLinkListener(iAppLinkListener: IAppLinkListener?) {
        var alreadyExists = false
        for (mAppLinkListener in mAppLinkListeners) {
            if (mAppLinkListener === iAppLinkListener) {
                alreadyExists = true
                break
            }
        }
        if (!alreadyExists) {
            mAppLinkListeners.add(iAppLinkListener)
        } else {
            // remove the old listener and add the new one
            mAppLinkListeners.remove(iAppLinkListener)
            mAppLinkListeners.add(iAppLinkListener)
        }
    }

    fun addModelListener(iModelListener: IModelListener?) {
        var alreadyExists = false
        val it: Iterator<IModelListener?> = mModelListeners.iterator()
        for (mModelListener in mModelListeners) {
            if (mModelListener === iModelListener) {
                alreadyExists = true
                break
            }
        }
        if (!alreadyExists) {
            mModelListeners.add(iModelListener)
        }
    }

    fun removeAppLinkListeners() {
    }

    fun removeModelListeners() {
        mModelListeners.clear()
    }

    val TAG: String = MessageMgr::class.simpleName!! // NOT an anonymous object

    private var mContext: Application? = null

    // When there's multiple cameras initializing at a time, it becomes a bit of a race condition.
    @JvmStatic
    var sdkStatus: AppLinkState = AppLinkState.APP_LINK_OFFLINE
        private set

    // JNI use
    @JvmStatic
    @Suppress("unused")
    private fun onModelMessage(
        deviceId: String,
        id: Long,
        type: Int,
        error: Int,
        path: String,
        data: String
    ) {
        //LogUtils.i(TAG, "onModelMessage deviceId:" + deviceId + ", id:" + id + ", type:" + type + ", error:" + error + ", path:" + path + ", data:" + data);
        dispatchModelMessage(
            ModelMessage(
                deviceId,
                id,
                type,
                error,
                path,
                data
            )
        )
    }

    // JNI use
    @JvmStatic
    @Suppress("unused")
    private fun onEventMessage(topic: String, data: String) {
        LogUtils.i(TAG, "onEventMessage topic:$topic, data:$data")
        dispatchEventMessage(
            EventMessage(
                MessageType.MSG_TYPE_EVENT,
                topic,
                data
            )
        )
    }

    // JNI use
    @JvmStatic
    @Suppress("unused")
    private fun onDataMessage(
        deviceId: String,
        id: Long,
        type: Int,
        error: Int,
        data: ByteArray
    ) {
        LogUtils.i(
            TAG,
            "onDataMessage deviceId = " + deviceId + ", id " + id + ", type:" + type + ", error:" + error + "; data:" + data.contentToString()
        )
    }

    //mSubscribeDeviceMap
    fun addSubscribeDevice(deviceIndex: Int, iResultListener: IResultListener<Boolean>) {
        mSubscribeDeviceMap[deviceIndex] = iResultListener
    }

    fun removeSubscribeDevice(iResultListener: IResultListener<Boolean>) {
        mSubscribeDeviceMap.entries.removeIf { it.value == iResultListener }
    }

    fun deviceSubscribed(iResultListener: IResultListener<Boolean>): Boolean {
        return mSubscribeDeviceMap.containsValue(iResultListener)
    }

    // This gets called by JNI when the app link state changes
    @JvmStatic
    @Suppress("unused")
    private fun onSubscribeDevice(deviceIndex: Int, error: Int) {
        // todo: get the error code value if possible
        LogUtils.i(TAG, "onSubscribeDevice ==========start==========")

        // if the map is empty, we don't have a listener to call
        if (!mSubscribeDeviceMap.isEmpty()) {
            val currentResListener: IResultListener<Boolean> =
                mSubscribeDeviceMap[deviceIndex]
                    ?: run {
                        // log that the listener is null and return
                        LogUtils.w(TAG, "onSubscribeDevice listener is null")
                        return
                    }

            // we have a listener, so we can call it
            LogUtils.i(TAG, "onSubscribeDevice: deviceIndex: $deviceIndex error: $error")

            // if there's an error, we need to call onError
            if (error != 0) {

                val onErrorAction = Runnable {
                    LogUtils.i(TAG, "onSubscribeDevice app status")
                    currentResListener.onError(error, "subscribe device error$error")
                }

                if (isMainThread) {
                    onErrorAction.run()
                } else {
                    mMainHandler.post(onErrorAction)
                }

                LogUtils.i(TAG, "onSubscribeDevice ==========end==========")
                return
            }

            val onSuccessAction = Runnable {
                LogUtils.i(TAG, "onSubscribeDevice app status")
                currentResListener.onSuccess(true)
                mSubscribeDeviceMap.remove(deviceIndex)
            }

            if (isMainThread) {
                onSuccessAction.run()
            } else {
                mMainHandler.post(onSuccessAction)
            }
        }
        LogUtils.i(TAG, "onSubscribeDevice ==========end==========")
    }

    // JNI use. I've never seen this called, but it's existence and signature
    // is validated on native_register in libiotvideo
    @JvmStatic
    @Suppress("unused")
    private fun onNetworkDetect(
        fromIp: Int,
        totalSent: Int,
        totalRec: Int,
        pingDelay: IntArray,
        resv: Int
    ): Int {
        val ret =
            "onNetworkDetect fromIp: " + fromIp + ", totalSend: " + totalSent + ", totalRecv: " + totalRec + ", pingDelay: " + pingDelay.contentToString() + ", resv: " + resv
        LogUtils.i(TAG, ret)
        return 0
    }

    // JNI use
    @JvmStatic
    @Suppress("unused")
    private fun ivCommonGetCb(type: Int, valueOut: ByteArray, minimumLength: Int): Int {
        LogUtils.i(
            TAG,
            String.format(
                Locale.getDefault(),
                "ivCommonGetCb type:%d,  len:%d, minimum:%d",
                type,
                minimumLength,
                minimumLength
            )
        )
        val readFile2BytesByStream = readFile2BytesByStream(getP2PSavePath(type))
        System.arraycopy(
            readFile2BytesByStream,
            0,
            valueOut,
            0,
            min(minimumLength.toDouble(), readFile2BytesByStream.size.toDouble())
                .toInt()
        )
        return min(minimumLength.toDouble(), readFile2BytesByStream.size.toDouble())
            .toInt()
    }

    // JNI use
    @JvmStatic
    @Suppress("unused")
    private fun ivCommonSetCb(type: Int, value: ByteArray, length: Int): Int {
        LogUtils.d(
            TAG,
            String.format(
                Locale.getDefault(),
                "ivCommonSetCb type:%d, data:%s, len:%d",
                type,
                value.contentToString(),
                length
            )
        )
        if (writeFileFromBytesByStream(getP2PSavePath(type), value)) {
            return 0
        }
        return -1
    }

    @SuppressLint("NewApi")
    private fun getP2PSavePath(type: Int): String {
        val absolutePath = mContext!!.filesDir.absolutePath
        val filePath = Paths.get(absolutePath, "p2pSave", "$type.p2p")
        val folderObject = filePath.parent.toFile()
        if (!folderObject.exists()) {
            LogUtils.i(TAG, "Creating p2p folder: $folderObject")
            try {
                folderObject.mkdirs()
            } catch (e: Exception) {
                LogUtils.e(TAG, "Failed to create p2p folder: $folderObject", e)
            }
        }
        return filePath.toString()
    }

    // This gets called by JNI when the app link state changes
    @JvmStatic
    @Suppress("unused")
    private fun onAppLinkStateChanged(status: Int) {
        val state = fromInt(status)
        if (sdkStatus != state) LogUtils.d(
            TAG,
            "App link state changed from $sdkStatus to $state"
        )
        sdkStatus = state

        if (mAppLinkListeners.isEmpty()) {
            LogUtils.w(TAG, "no app link listeners to send to! status: $sdkStatus")
        } else {

            val stateAction = Runnable {
                for (mAppLinkListener in mAppLinkListeners) {
                    mAppLinkListener!!.onAppLinkStateChanged(state)
                }
            }

            if (isMainThread) {
                stateAction.run()
            } else {
                mMainHandler.post(stateAction)
            }
        }
    }


    private val isMainThread: Boolean
        get() = Looper.getMainLooper().thread === Thread.currentThread()

    @JvmStatic
    @Suppress("unused") // JNI, calls this _often_
    private fun getLocalNetIpFromP2p(ipAddressBytes: ByteArray, reservedBytes: ByteArray) {
//        LogUtils.i(TAG, "getLocalNetIpFromP2p called");
        try {
            val split = IoTVideoSdk.localIPAddress.split("\\.".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (4 == split.size) {
                val min = min(ipAddressBytes.size.toDouble(), split.size.toDouble())
                    .toInt()
                for (i in 0 until min) {
                    var intValue = split[i].toInt()
                    if (intValue > 127) {
                        intValue -= 256
                    }
                    ipAddressBytes[i] = intValue.toByte()
                }
                Arrays.fill(reservedBytes, 0.toByte())
                return
            }
            LogUtils.e(TAG, "Net list is invalid")
            Arrays.fill(ipAddressBytes, 0.toByte())
            Arrays.fill(reservedBytes, 0.toByte())
        } catch (e: Exception) {
            logStackTrace(TAG, e.message ?: "getLocalNetIpFromP2p")
            Arrays.fill(ipAddressBytes, 0.toByte())
            Arrays.fill(reservedBytes, 0.toByte())
        }
    }
}