package com.tencentcs.iotvideo

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.text.TextUtils
import android.util.Log
import com.github.xerootg.cryze.httpclient.responses.AccessCredential
import com.tencent.mars.xlog.LogLevel
import com.tencentcs.iotvideo.iotvideoplayer.Mode
import com.tencentcs.iotvideo.messagemgr.MessageMgr.register
import com.tencentcs.iotvideo.messagemgr.MessageMgr.sdkStatus
import com.tencentcs.iotvideo.messagemgr.MessageMgr.unregister
import com.tencentcs.iotvideo.netconfig.Language
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.Utils
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

object IoTVideoSdk {
    fun unRegister() {
        if (registerState != SDK_REGISTER_STATE_UNREGISTERED) {
            unRegister(false)
        }
    }

    private const val IOT_HOST: String = "|wyze-mars-asrv.wyzecam.com"
    private var mNetWorkStateReceiver: NetWorkStateReceiver? = null

    const val APP_LINK_ACCESS_TOKEN_ERROR: Int = 3
    const val APP_LINK_DEV_DISABLE: Int = 7
    const val APP_LINK_INVALID_TID: Int = 5
    const val APP_LINK_KICK_OFF: Int = 6
    const val APP_LINK_OFFLINE: Int = 2
    const val APP_LINK_ONLINE: Int = 1
    const val APP_LINK_TID_INIT_ERROR: Int = 4
    const val DEV_TYPE_DID: Int = 2
    const val DEV_TYPE_THIRD_ID: Int = 3
    const val DEV_TYPE_TID: Int = 1
    const val LAN_DEV_CONNECTABLE_STATE_COULD_NOT_USE: Int = 0
    const val LAN_DEV_CONNECTABLE_STATE_COULD_USE: Int = 1
    const val LAN_DEV_CONNECTABLE_STATE_NOT_INIT: Int = -1
    const val LAN_DEV_CONNECTABLE_STATE_PARAMS_INVALID: Int = -3
    const val LAN_DEV_CONNECTABLE_STATE_USER_TYPE_INVALID: Int = -2
    const val PREFIX_THIRD_ID: String = "_@."
    const val SDK_REGISTER_STATE_IDL: Int = 0
    const val SDK_REGISTER_STATE_REGISTERED: Int = 2
    const val SDK_REGISTER_STATE_REGISTERING: Int = 1
    const val SDK_REGISTER_STATE_UNREGISTERED: Int = 4
    const val SDK_REGISTER_STATE_UNREGISTERING: Int = 3
    private val TAG = IoTVideoSdk::class.simpleName
    private const val VALID_REGISTER_TIME_INTERVAL: Long = 1000
    private var lastRegisterAppSysLanguage: Language = Language.NONE
    private var lastRegisterDeviceType = 0
    private var mContext: Application? = null
    private var mReceiverLock: Lock
    var registerState: Int = 0
        private set
    private var supportedAbi: Array<String> = Build.SUPPORTED_ABIS
    private var sUserInfo: HashMap<String, Any>? = HashMap()
    private var lastRegisterTime: Long = 0
    private var lastRegisterAccessId: Long = -1
    private var lastRegisterAccessToken: String? = ""
    private var isWaitingUnregister = false
    var sdkInited: Boolean = false
        private set

    init {
        if (isSupportedCurrentAbi) {
            try {
                System.loadLibrary("mbedtls")
                System.loadLibrary("iotp2pav")
                System.loadLibrary("iotvideo")
            } catch (err: InternalError) {
                Log.e(TAG, "iot video NOT sdk statically created! - could not load libraries")
            }
            Log.i(TAG, "iot video sdk statically created!")
        } else {
            Log.e(TAG, "iot video NOT sdk statically created!")
        }
        mReceiverLock = ReentrantLock()
    }

    fun init(application: Application?, options: HashMap<String, Any>?) {
        val isSupportedCurrentAbi = isSupportedCurrentAbi
        supportedAbi = Utils.supportedAbi
        LogUtils.i(TAG, "init version is 1.0.0; supported Abi: " + supportedAbi.contentToString())
        sdkInited = isSupportedCurrentAbi
        if (!isSupportedCurrentAbi) {
            return
        }
        mContext = application
        sUserInfo = options
        if (options == null) {
            sUserInfo = HashMap()
        }
        Log.i(TAG, "init successful")
    }

    private fun registerNetBroadcastReceiver() {
        mReceiverLock.lock()
        if (mNetWorkStateReceiver == null) {
            val intentFilter = IntentFilter()
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
            val netWorkStateReceiver = NetWorkStateReceiver()
            mNetWorkStateReceiver = netWorkStateReceiver
            mContext!!.registerReceiver(netWorkStateReceiver, intentFilter)
            LogUtils.i(TAG, "registerNetBroadcastReceiver")
        }
        mReceiverLock.unlock()
    }


    fun register(accessCredential: AccessCredential) {
        register(accessCredential.accessId, accessCredential.accessToken, DEV_TYPE_THIRD_ID)
    }

    @JvmOverloads
    fun register(
        accessId: Long,
        accessToken: String?,
        deviceType: Int = DEV_TYPE_TID,
        sysLanguage: Language = Language.English
    ) {
        LogUtils.e(TAG, "IotVideoSdk Register Started")
        if (!sdkInited) {
            LogUtils.e(TAG, "register sdk error: sdk is not init")
        } else if (isSupportedCurrentAbi) {
            LogUtils.i(TAG, "Starting IoTVideoSdk register!")
            lastRegisterAccessId = accessId
            lastRegisterAccessToken = accessToken
            lastRegisterDeviceType = deviceType
            lastRegisterAppSysLanguage = sysLanguage
            lastRegisterTime = System.currentTimeMillis()
            LogUtils.i(TAG, "register registerState:$registerState")
            if (registerState != SDK_REGISTER_STATE_IDL && SDK_REGISTER_STATE_UNREGISTERED != registerState) //SDK_REGISTER_STATE_REGISTERED
            {
                LogUtils.i(TAG, "register,auto unregister")
                isWaitingUnregister = true
                unRegister(true)
                return
            }
            isWaitingUnregister = false
            registerState = SDK_REGISTER_STATE_REGISTERING
            register(mContext)
            val split = "1.0.0".split("\\(".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val versionCode =
                1 or (split[1].toInt() shl 19) or (split[0].toInt() shl 23) or 268435456
            val p2pPortType = Mode.P2P.ordinal
            LogUtils.i(TAG, "register p2pPortType:$p2pPortType net:$localIPAddress")
            nativeRegister(
                accessId,
                accessToken,
                IOT_HOST,
                versionCode,
                deviceType.toShort(),
                sysLanguage.language.toShort(),
                p2pPortType
            )
            LogUtils.i(
                TAG,
                "register accessId: $accessId, accessToken: $accessToken, version = 0x ${Integer.toHexString(versionCode)}, p2pUrl: $IOT_HOST language: ${sysLanguage.name}"
            )

            registerNetBroadcastReceiver()
            registerState = SDK_REGISTER_STATE_REGISTERED
        }
    }

    private fun unRegister(autoRegister: Boolean) {
        LogUtils.i(TAG, "unregister, isAuto:$autoRegister")
        if (isSupportedCurrentAbi) {
            if (!sdkInited) {
                LogUtils.e(TAG, "unregister error:sdk is not init")
                return
            }
            if (!autoRegister) {
                lastRegisterTime = 0L
                lastRegisterAccessId = -1L
                lastRegisterAccessToken = null
                lastRegisterDeviceType = 0
                lastRegisterAppSysLanguage = Language.NONE
                isWaitingUnregister = false
            }
            registerState = 3
            unregister(!autoRegister)
            val newSingleThreadExecutor = Executors.newSingleThreadExecutor()
            newSingleThreadExecutor.execute(Runnable
            {
                nativeUnregister()
                registerState = SDK_REGISTER_STATE_UNREGISTERED
                LogUtils.i(TAG, "unregister finish, isWaitingUnregister:$isWaitingUnregister")
                if (isWaitingUnregister) {
                    if (-1L != lastRegisterAccessId && !TextUtils.isEmpty(
                            lastRegisterAccessToken
                        )
                    ) {
                        LogUtils.i(TAG, "register after auto unregister")
                        lastRegisterTime = 0L
                        register(
                            lastRegisterAccessId,
                            lastRegisterAccessToken,
                            lastRegisterDeviceType,
                            lastRegisterAppSysLanguage
                        )
                        return@Runnable
                    }
                    LogUtils.e(
                        TAG,
                        "register after auto unregister error:input params is invalid"
                    )
                }
            })
            newSingleThreadExecutor.shutdown()
            unregisterNetBroadcastReceiver()
        }
    }

    private fun unregisterNetBroadcastReceiver() {
        mReceiverLock.lock()
        if (mNetWorkStateReceiver != null) {
            LogUtils.i(TAG, "unregisterNetBroadcastReceiver")
            mContext!!.unregisterReceiver(mNetWorkStateReceiver)
            mNetWorkStateReceiver = null
        }
        mReceiverLock.unlock()
    }

    val isSupportedCurrentAbi: Boolean
        get() {
            if(
                supportedAbi.contains("arm64-v8a")
                || supportedAbi.contains("armeabi-v7a")
                || supportedAbi.contains("armeabi"))
            {
                return true
            }
            return false
        }

    fun lanDevConnectable(deviceId: String?): Int {
        if (!sdkInited) {
            return LAN_DEV_CONNECTABLE_STATE_NOT_INIT
        }
        if (TextUtils.isEmpty(deviceId)) {
            LogUtils.e(TAG, "isLanDevConnectable error:input params is invalid")
            return LAN_DEV_CONNECTABLE_STATE_PARAMS_INVALID
        }
        return nIsLanDevConnectable(deviceId)
    }

    // The consolidated logLevel value is the inverse of the XLog level
    fun setDebugMode(level: LogLevel): Int {
        return setDebugMode(level.toSdkLevel())
    }
    // Only used with LogLevel, as above.
    private external fun setDebugMode(level: Int): Int

    external fun setLogPath(path: String): Int

    // All JNI required, but unused by us
    @Suppress("unused")
    private external fun nativeGetRstpPassword(str: String): ByteArray?
    @Suppress("unused")
    private external fun nativeCheckAndSetDevicePwd(str: String, str2: String, str3: String): Int
    @Suppress("unused")
    private external fun nativeGetAnonymousSecureKey(str: String, j10: Long): Array<String?>?
    @Suppress("unused")
    private external fun nativeGetTerminalId(): Long
    @Suppress("unused")
    private external fun nativeSha256WithHex(str: String): String?
    @Suppress("unused")
    private external fun nativeSha1WithBase256(str: String, str2: String): String?
    @Suppress("unused")
    private external fun uncompressStr(bArr: ByteArray, i10: Int): ByteArray?
    @Suppress("unused")
    private external fun nativeStartDetectDevNetwork(str: String, str2: String, i10: Int, i11: Int)
    @Suppress("unused")
    private external fun nativeUpdateToken(str: String): Int
    @Suppress("unused")
    private external fun nGetDidFromTid(str: String): Long
    @Suppress("unused")
    private external fun nGetHLSHttpPort(): Int


    private external fun nativeGetP2PVersion(): Int
    private external fun nIsLanDevConnectable(str: String?): Int
    val p2PVersion: Int
        get() = nativeGetP2PVersion()
    private external fun nativeRegister(
        accessId: Long,
        accessToken: String?,
        host: String,
        versionCode: Int,
        deviceType: Short,
        language: Short,
        p2pPortType: Int
    )
    private external fun nativeUnregister()
    external fun onNetworkChanged()

    // TODO: Find a better way to do this, maybe even be NAT compatible for p2p
    val localIPAddress: String
        get() {
            val networkInterfaces: Enumeration<NetworkInterface?>?
            try {
                networkInterfaces = NetworkInterface.getNetworkInterfaces()
            } catch (e: SocketException) {
                throw RuntimeException(e)
            }
            if (networkInterfaces == null) {
                return null.toString()
            }
            while (networkInterfaces.hasMoreElements()) {
                val nextElement = networkInterfaces.nextElement()
                if (nextElement != null) {
                    val inetAddresses = nextElement.inetAddresses
                    while (inetAddresses.hasMoreElements()) {
                        val nextElement2 = inetAddresses.nextElement()
                        if (!nextElement2.isLoopbackAddress && (nextElement2 is Inet4Address)) {
                            // if it's Inet4Address, it's not null. but hey, maybe it is.
                            return nextElement2.getHostAddress() ?: "null"
                        }
                    }
                    continue
                }
            }
            return null.toString()
        }

    class NetWorkStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if ("android.net.conn.CONNECTIVITY_CHANGE" == intent.action) {
                LogUtils.i(TAG, "Connectivity Change")
                if (!isSupportedCurrentAbi) {
                    LogUtils.e(TAG, "onReceive don't support abi")
                } else if (sdkStatus != AppLinkState.APP_LINK_ONLINE) {
                    LogUtils.i(TAG, "app is not online")
                } else {
                    val newSingleThreadExecutor = Executors.newSingleThreadExecutor()
                    newSingleThreadExecutor.execute {
                        if (3 != registerState) {
                            onNetworkChanged()
                        } else {
                            LogUtils.e(TAG, "onReceive failure")
                        }
                    }
                    newSingleThreadExecutor.shutdown()
                }
            }
        }
    }
}


