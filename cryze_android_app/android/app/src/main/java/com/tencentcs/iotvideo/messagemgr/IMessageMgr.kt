package com.tencentcs.iotvideo.messagemgr

import com.tencentcs.iotvideo.utils.rxjava.IResultListener
import java.util.concurrent.ConcurrentHashMap

//TODO: pick through the JNI libraries and whittle this down to what's needed.
interface IMessageMgr {
    fun addAppLinkListener(iAppLinkListener: IAppLinkListener?)

    fun addEventListener(iEventListener: IEventListener?)

    fun addModelListener(iModelListener: IModelListener?)

    fun notifyDevModelToServer(str: String?, str2: String?)

    fun operateProUser(
        str: String?,
        str2: String?,
        str3: String?,
        str4: String?,
        i10: Int,
        iResultListener: IResultListener<ModelMessage?>?
    )

    fun operateProUser(
        str: String?,
        str2: String?,
        str3: String?,
        str4: String?,
        iResultListener: IResultListener<ModelMessage?>?
    )

    fun readProperty(
        str: String?,
        str2: String?,
        i10: Int,
        iResultListener: IResultListener<ModelMessage?>?
    )

    fun readProperty(str: String?, str2: String?, iResultListener: IResultListener<ModelMessage?>?)

    fun removeAppLinkListener(iAppLinkListener: IAppLinkListener?)

    fun removeAppLinkListeners()

    fun removeEventListener(iEventListener: IEventListener?)

    fun removeEventListeners()

    fun removeModelListener(iModelListener: IModelListener?)

    fun removeModelListeners()

    fun sendDataToDevice(
        str: String?,
        b10: Byte,
        bArr: ByteArray?,
        z10: Boolean,
        i10: Int,
        iResultListener: IResultListener<DataMessage?>?
    )

    fun sendDataToDevice(
        str: String?,
        bArr: ByteArray?,
        z10: Boolean,
        i10: Int,
        iResultListener: IResultListener<DataMessage?>?
    )

    fun sendDataToDevice(
        str: String?,
        bArr: ByteArray?,
        z10: Boolean,
        i10: Int,
        z11: Boolean,
        iResultListener: IResultListener<DataMessage?>?
    )

    fun sendDataToDeviceWithResponse(
        str: String?,
        b10: Byte,
        bArr: ByteArray?,
        iResultListener: IResultListener<DataMessage?>?
    )

    fun sendDataToDeviceWithResponse(
        str: String?,
        bArr: ByteArray?,
        iResultListener: IResultListener<DataMessage?>?
    )

    fun sendDataToDeviceWithoutResponse(
        str: String?,
        b10: Byte,
        bArr: ByteArray?,
        iResultListener: IResultListener<DataMessage?>?
    )

    fun sendDataToDeviceWithoutResponse(
        str: String?,
        bArr: ByteArray?,
        iResultListener: IResultListener<DataMessage?>?
    )

    fun sendDataToServer(
        str: String?,
        bArr: ByteArray?,
        i10: Int,
        iResultListener: IResultListener<DataMessage?>?
    )

    fun sendDataToServer(
        str: String?,
        bArr: ByteArray?,
        iResultListener: IResultListener<DataMessage?>?
    )

    fun subscribeDevice(concurrentHashMap: ConcurrentHashMap<Int, IResultListener<Boolean>>?)

    fun writeProperty(
        str: String?,
        str2: String?,
        str3: String?,
        i10: Int,
        iResultListener: IResultListener<ModelMessage?>?
    )

    fun writeProperty(
        str: String?,
        str2: String?,
        str3: String?,
        iResultListener: IResultListener<ModelMessage?>?
    )
}
