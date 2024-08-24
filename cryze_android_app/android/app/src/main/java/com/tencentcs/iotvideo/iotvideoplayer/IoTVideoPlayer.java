package com.tencentcs.iotvideo.iotvideoplayer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.tencentcs.iotvideo.IoTVideoErrors;
import com.tencentcs.iotvideo.IoTVideoSdk;
import com.tencentcs.iotvideo.StackTraceUtils;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioDecoder;
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioEncoder;
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder;
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoEncoder;
import com.tencentcs.iotvideo.iotvideoplayer.player.IIoTVideoPlayer;
import com.tencentcs.iotvideo.iotvideoplayer.player.PlayerUserData;
import com.tencentcs.iotvideo.iotvideoplayer.render.AudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderGroup;
import com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer;
import com.tencentcs.iotvideo.messagemgr.IFileDownloadInnerDataListener;
import com.tencentcs.iotvideo.messagemgr.IMonitorInnerUserDataLister;
import com.tencentcs.iotvideo.messagemgr.IPlaybackInnerUserDataLister;
import com.tencentcs.iotvideo.messagemgr.InnerUserDataHandler;
import com.tencentcs.iotvideo.utils.ByteUtils;
import com.tencentcs.iotvideo.utils.LogUtils;
import com.tencentcs.iotvideo.utils.rxjava.IResultListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IoTVideoPlayer implements IIoTVideoPlayer {

    private static final long DEFAULT_AUDIO_RENDER_ID = -1;
    private static final long LOOP_GET_AV_BYTES_TIME_INTERVAL = 1000;
    private static final byte SET_USER_DATA_CHANGE_DEFINITION_CMD = 5;
    private static final byte SET_USER_DATA_PLAYBACK_PAUSE_CMD = 1;
    private static final byte SET_USER_DATA_PLAYBACK_RESUME_CMD = 2;
    private static final byte SET_USER_DATA_PLAYBACK_SEEK_CMD = 3;
    private static final int SET_USER_DATA_TO_INNER = 0;
    private static final int SET_USER_DATA_TO_OUTSIDE = 1;
    private static final String TAG = "IoTVideoPlayer";
    private static final String PREFIX_THIRD = "_@.";

    private AVHeader mAVHeader;
    private String wmPath;
    private ConcurrentLinkedQueue<Long> mSeekCmdList = new ConcurrentLinkedQueue<>();
    private long mAudioRenderId = -1;
    private boolean isRecordFail = false;
    private boolean isSupportEncode = true;

    public native int getVideoDefinition();
    private native byte[] nGetRenderAVData();
    private native void nSetAccelRenderMode(int i10);
    private native void nSetPlayerOption(int i10, String str, long j10);
    private native void nSetPlayerOption(int i10, String str, String str2);
    private native AVHeader nativeGetAVHeader();
    private native int[] nativeGetConnectMode();
    private native int nativeGetPlayState();
    private native void nativeInit();
    private native void nativeInitLastFramePath(String str);
    private native boolean nativeIsMute();
    private native boolean nativeIsRecording();
    private native void nativeMute(boolean z10);
    public native void nativePlay();
    public native void nativePlaybackSpeedRet(float f10);
    public native void nativePrepare();
    private native void nativeRelease();
    private native void nativeSeek(long j10);
    public native void nativeSeekRet(boolean z10, long j10);
    public native int nativeSendUserData(byte[] bArr, boolean z10);
    private native void nativeSetAudioDecoder(IAudioDecoder iAudioDecoder);
    private native void nativeSetAudioEncoder(IAudioEncoder iAudioEncoder);
    private native void nativeSetAudioRender(IAudioRender iAudioRender);
    private native void nativeSetConnectDevStateListener(IConnectDevStateListener iConnectDevStateListener);
    private native void nativeSetDataResource(String str, int i10, PlayerUserData playerUserData);
    private native void nativeSetDisplay(SurfaceHolder surfaceHolder);
    private native void nativeSetErrorListener(IErrorListener iErrorListener);
    private native void nativeSetOnReceiveChangedHeaderListener(OnReceiveAVHeaderListener onReceiveAVHeaderListener);
    private native void nativeSetPreparedListener(IPreparedListener iPreparedListener);
    private native void nativeSetRecordAudioEncoder(IAudioEncoder iAudioEncoder);
    private native void nativeSetRecordVideoEncoder(IVideoEncoder iVideoEncoder);
    private native void nativeSetStatusListener(IStatusListener iStatusListener);
    private native void nativeSetSurface(Surface surface);
    private native void nativeSetTimeListener(ITimeListener iTimeListener);
    private native void nativeSetUserDataListener(IUserDataListener iUserDataListener);
    private native void nativeSetVideoDecoder(IVideoDecoder iVideoDecoder);
    private native void nativeSetVideoEncoder(IVideoEncoder iVideoEncoder);
    private native void nativeSetVideoFramesListener(IVideoFramesListener iVideoFramesListener);
    private native void nativeSetVideoRender(IVideoRender iVideoRender);
    private native void nativeSnapShot(long j10, String str, ISnapShotListener iSnapShotListener);
    private native boolean nativeStartRecord(String str, IRecordListener iRecordListener);
    private native void nativeStop();
    private native void nativeStopRecord();
    private native void nativeWmPath(String str);
    public native void reviseNativeDefinition(int i10);
    public native void nativeCloseCamera();
    public native int nativeGetAvBytesPerSec();
    public native float nativeGetPlaybackSpeed();
    public native void nativeOpenCamera();
    public native void nativePause();
    public native void nativeResume();
    public native void nativeSendAudioData(AVData aVData);
    public native void nativeSendVideoData(AVData aVData);
    public native void nativeSetCaptureHeader(AVHeader aVHeader);
    public native void nativeSetVideoStuckStrategy(IVideoStuckStrategy iVideoStuckStrategy);
    public native void nativeStartTalk();
    public native void nativeStopTalk();
    public native void nativeUpdateAccessIdAndToken(long j10, String str);

    private long nativeObject = 0;
    private Handler mTaskTHandler;
    private HandlerThread mTaskThread;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private InnerUserDataHandler mInnerUserDataHandler;
    private IPreparedListener mPreparedListener;
    private IStatusListener mStatusListener;
    private ITimeListener mTimeListener;
    private IErrorListener mErrorListener;
    private IConnectDevStateListener mConnectDevStateListener;
    private IVideoFluctuationListener mVideoFluctuationListener;
    private IVideoFluctuationStrategy mVideoFluctuationStrategy;
    private IVideoRender mVideoRender;
    private AVHeader mReceiveAvHeader = null;
    private IVideoRenderFilter mVideoRenderFilter = null;
    private IAudioRender mAudioRender;
    private OnReceiveAVHeaderListener mOnReceiveAVHeaderListener;
    private IViewsCreator mViewsCreator;
    private IFileDownloadInnerDataListener mFileDownloadInnerDataListener;
    private IMonitorInnerUserDataLister mMonitorInnerUserDataLister;
    private IPlaybackInnerUserDataLister mPlaybackInnerUserDataLister;
    private AvReceiveRateListener mReceiveRateListener;

    private IUserDataListener mUserDataListener;

    private final Map<Byte, AVHeader> videoSizeMap = new HashMap();

    private byte waitSendDefinition = -1;
    private int mPlaybackPlayStrategy = 0;
    private byte currentDefinition = -1;


    public IoTVideoPlayer() {
        // init IotVideoSdk if needed
        if (!IoTVideoSdk.isInited())
        {
            LogUtils.w(TAG, "Iot sdk is not inited for player");
            return;
        }
        nativeInit();
        LogUtils.i(TAG, "java player created, javaObj: " + hashCode() + " nativeObject: " + Long.toHexString(this.nativeObject));
        HandlerThread handlerThread = new HandlerThread("IoTVideo-Thread");
        this.mTaskThread = handlerThread;
        handlerThread.start();
        this.mTaskTHandler = new Handler(this.mTaskThread.getLooper());
        this.mInnerUserDataHandler = new InnerUserDataHandler();

        nativeSetPreparedListener(new IPreparedListener() {
            @Override
            public void onPrepared() {
                if (mPreparedListener == null) {
                    return;
                }
                if (isMainThread()) {
                    mPreparedListener.onPrepared();
                } else {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mPreparedListener != null) {
                                mPreparedListener.onPrepared();
                            }
                        }
                    });
                }

                LogUtils.i(TAG, "onPrepared native listener fired!");
            }
        });

        nativeSetStatusListener(new IStatusListener() {
            @Override
            public void onStatus(int stat) {
                Object valueOf;
                onPlayerStateChange(stat);
                StringBuilder statusMessage = new StringBuilder("nativeSetStatusListener status :");
                statusMessage.append(stat);
                statusMessage.append("; listener:");
                if (mStatusListener == null) {
                    valueOf = "listener is null";
                } else {
                    valueOf = mStatusListener.hashCode();
                }
                statusMessage.append(valueOf);
                LogUtils.i(IoTVideoPlayer.TAG, statusMessage.toString());
                if (mStatusListener == null) {
                    return;
                }
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mStatusListener != null) {
                            mStatusListener.onStatus(stat);
                        }
                    }
                });
                LogUtils.i(TAG, "onStatus native listener fired!");
            }
        });

        nativeSetTimeListener(new ITimeListener() {
            @Override
            public void onTime(long j10) {
                if (mTimeListener == null) {
                    return;
                }
                if (isMainThread()) {
                    mTimeListener.onTime(j10);
                } else {
                    mMainHandler.post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer.3.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (mTimeListener != null) {
                                mTimeListener.onTime(j10);
                            }
                        }
                    });
                }

                LogUtils.i(TAG, "onTime native listener fired!");
            }
        });

        nativeSetErrorListener(new IErrorListener() {
            @Override
            public void onError(int i10) {

                if (mErrorListener == null) {
                    return;
                }
                if (isMainThread()) {
                    mErrorListener.onError(i10);
                } else {
                    mMainHandler.post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer.4.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (mErrorListener != null) {
                                mErrorListener.onError(i10);
                            }
                        }
                    });
                }

                LogUtils.i(TAG, "onError native listener fired!");
            }
        });

        nativeSetUserDataListener(new IUserDataListener() {
            @Override
            public void onReceive(byte[] bArr) {
                LogUtils.i(TAG, "onReceive native listener fired!");
            }
        });

        nativeSetVideoRender(new AVideoRender() {
            @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
            public void onFrameUpdate(AVData aVData) {
                LogUtils.d(IoTVideoPlayer.TAG, "nativeSetVideoRender onFrameUpdate");
                if (IoTVideoPlayer.this.mVideoRender == null) {
                    return;
                }
                if (mVideoRenderFilter != null && (aVData = mVideoRenderFilter.videoFilter(aVData)) == null) {
                    LogUtils.i(IoTVideoPlayer.TAG, "onFrameUpdate failure:video filter return null data");
                } else if (aVData != null) {
                    IoTVideoPlayer.this.mVideoRender.onFrameUpdate(aVData);
                }
            }

            @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
            public void onInit(AVHeader aVHeader) {
                LogUtils.i(IoTVideoPlayer.TAG, "nativeSetVideoRender jni: AVideoRender.onInit(..), header = " + aVHeader);
                if (mReceiveAvHeader != null && mVideoRender != null) {
                    mVideoRender.onInit(mReceiveAvHeader);
                }
            }

            @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
            public void onRelease() {
                LogUtils.i(IoTVideoPlayer.TAG, "nativeSetVideoRender jni: AVideoRender.onRelease(..)");
                if (mVideoRender != null) {
                    mVideoRender.onRelease();
                }
            }
        });
        nativeSetAudioRender(new IAudioRender() {
            @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
            public void flushRender() {
                if (mAudioRender == null) {
                    return;
                }
                mAudioRender.flushRender();
            }

            @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
            public long getWaitRenderDuration() {
                if (mAudioRender != null) {
                    return mAudioRender.getWaitRenderDuration();
                }
                return 0L;
            }

            @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
            public void onFrameUpdate(AVData aVData) {
//                LogUtils.i(IoTVideoPlayer.TAG, "nativeIAudioRender onFrameUpdate");

                if (mAudioRender == null) {
                    return;
                }
                mAudioRender.onFrameUpdate(aVData);
            }

            @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
            public void onInit(AVHeader aVHeader) {
                LogUtils.i(IoTVideoPlayer.TAG, "jni: IAudioRender.onInit(..), header = " + aVHeader);
                if (mReceiveAvHeader != null && mAudioRender != null) {
                    mAudioRender.onInit(aVHeader);
                }
            }

            @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
            public void onRelease() {
                LogUtils.i(IoTVideoPlayer.TAG, "jni: IAudioRender.onRelease()");
                if (mAudioRender != null) {
                    mAudioRender.onRelease();
                }
            }

            @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
            public void setPlayerVolume(float f) {
            }
        });

        nativeSetConnectDevStateListener(new IConnectDevStateListener() {
            @Override
            public void onStatus(int i10) {
                if (mConnectDevStateListener == null) {
                    return;
                }
                if (isMainThread()) {
                    mConnectDevStateListener.onStatus(i10);
                } else {
                    mMainHandler.post(new Runnable() {
                        @Override // java.lang.Runnable
                        public void run() {
                            if (mConnectDevStateListener != null) {
                                mConnectDevStateListener.onStatus(i10);
                            }
                        }
                    });
                }
                LogUtils.i(TAG, "IConnectDevStateListener onStatus native listener fired!");
            }
        });

        nativeSetVideoFramesListener(new IVideoFramesListener() {
            @Override
            public void onReceiveVideoFramesPerSecond(int i10, int i11) {
                final int calculateStrategy;
//                LogUtils.i(IoTVideoPlayer.TAG, "onReceiveVideoFramesPerSecond onFrameUpdate: " + i10 + ", " + i11);

                if (mVideoFluctuationListener == null || mVideoFluctuationStrategy == null || (calculateStrategy = mVideoFluctuationStrategy.calculateStrategy(i10, i11, getVideoDefinition())) < 0) {
                    return;
                }
                if (isMainThread()) {
                    mVideoFluctuationListener.onVideoFluctuation(calculateStrategy);
                } else {
                    mMainHandler.post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer.9.1
                        @Override // java.lang.Runnable
                        public void run() {
                            mVideoFluctuationListener.onVideoFluctuation(calculateStrategy);
                        }
                    });
                }
                LogUtils.i(TAG, "IVideoFramesListener onReceiveVideoFramesPerSecond native listener fired!");
            }
        });

        nativeSetOnReceiveChangedHeaderListener(new OnReceiveAVHeaderListener() {
            @Override
            public void onReceiveChangedAVHeader(AVHeader aVHeader) {
                LogUtils.d(IoTVideoPlayer.TAG, "onReceiveChangedAVHeader(..), AVHeader = " + aVHeader);
                if (aVHeader == null) {
                    return;
                }
                if (mReceiveAvHeader == null) {
                    mReceiveAvHeader = new AVHeader();
                }
                mReceiveAvHeader.copy(aVHeader);
                if (isMainThread()) {
                    localOnReceiveChangedAVHeader(mReceiveAvHeader);
                    return;
                }
                mMainHandler.post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer.10.1
                    @Override // java.lang.Runnable
                    public void run() {
                        localOnReceiveChangedAVHeader(mReceiveAvHeader);
                    }
                });
            }
        });
    }

    public void localOnReceiveChangedAVHeader(AVHeader aVHeader) {
        OnReceiveAVHeaderListener onReceiveAVHeaderListener = this.mOnReceiveAVHeaderListener;
        if (onReceiveAVHeaderListener != null) {
            onReceiveAVHeaderListener.onReceiveChangedAVHeader(aVHeader);
        }
        IViewsCreator iViewsCreator = this.mViewsCreator;
        if (iViewsCreator != null) {
            setVideoView(iViewsCreator.onCreateViews(aVHeader));
        }
    }

    public void setConnectDevStateListener(IConnectDevStateListener iConnectDevStateListener) {
        this.mConnectDevStateListener = iConnectDevStateListener;
    }

    public void onPlayerStateChange(int i) {
        byte b;
        LogUtils.i(TAG, "onPlayerStateChange playerState:" + i);
        if (!isConnectedDevice()) {
            this.mPlaybackPlayStrategy = 0;
        }
        if (isConnectedDevice() && (b = this.waitSendDefinition) >= 0) {
            changeDefinition(b, null);
        }
        loopGetTransmissionRate(isConnectedDevice());
    }

    public void changeDefinition(byte b, IResultListener<Boolean> iResultListener) {
        changeDefinition(b, true, iResultListener);
    }

    public void changeDefinition(final byte definition, final boolean z, final IResultListener<Boolean> iResultListener) {
        LogUtils.i(TAG, String.format("changeDefinition nativeObject:0x%s; definition:%d", Long.toHexString(this.nativeObject), Byte.valueOf(definition)));
        this.currentDefinition = definition;
        if (!isConnectedDevice()) {
            LogUtils.i(TAG, "changeDefinition ret:disconnect with device, waiting connecting dev, player status:" + getPlayState());
            if (iResultListener != null) {
                iResultListener.onSuccess(Boolean.TRUE);
            }
            if (2 == getPlayState()) {
                this.waitSendDefinition = definition;
                LogUtils.i(TAG, "changeDefinition is connecting device");
            }
            reviseNativeDefinition(definition);
            return;
        }
        this.waitSendDefinition = (byte) -1;
        IResultListener<byte[]> iResultListener2 = new IResultListener<byte[]>() { // from class: com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer.16
            @Override // com.tencentcs.iotvideo.utils.rxjava.IResultListener
            public void onError(int errorCode, String message) {
                IResultListener iResultListener3 = iResultListener;
                if (iResultListener3 != null && z) {
                    iResultListener3.onError(errorCode, message);
                }
                LogUtils.i(IoTVideoPlayer.TAG, "changeDefinition onError definition: " + definition + "; errorcode: " + errorCode + "; errormsg: " + message);
            }

            @Override // com.tencentcs.iotvideo.utils.rxjava.IResultListener
            public void onStart() {
                IResultListener iResultListener3 = iResultListener;
                if (iResultListener3 != null && z) {
                    iResultListener3.onStart();
                }
            }

            @Override // com.tencentcs.iotvideo.utils.rxjava.IResultListener
            public void onSuccess(byte[] success) {
                if (!z) {
                    LogUtils.i(IoTVideoPlayer.TAG, "changeDefinition onSuccess, haveNotifyFromDev:" + z);
                } else if (success != null && success.length > 0 && 255 != (success[0] & 255)) {
                    IResultListener iResultListener3 = iResultListener;
                    if (iResultListener3 != null) {
                        iResultListener3.onSuccess(Boolean.TRUE);
                    }
                    reviseNativeDefinition(definition);
                    LogUtils.i(IoTVideoPlayer.TAG, "changeDefinition onSuccess definition:" + definition);
                } else {
                    IResultListener iResultListener4 = iResultListener;
                    if (iResultListener4 != null) {
                        iResultListener4.onError(IoTVideoErrors.ERROR_RESULT, null);
                    }
                    LogUtils.i(IoTVideoPlayer.TAG, "changeDefinition error:" + Arrays.toString(success));
                }
            }
        };
        this.videoSizeMap.put(Byte.valueOf(this.currentDefinition), null);
        sendInnerUserData((byte) 5, new byte[]{definition}, iResultListener2);
        if (z) {
            return;
        }
        if (iResultListener != null) {
            iResultListener.onSuccess(Boolean.TRUE);
        }
        reviseNativeDefinition(this.currentDefinition);
    }

    private void sendInnerUserData(byte b, byte[] bArr, IResultListener<byte[]> iResultListener) {
        sendInnerUserData(b, bArr, false, iResultListener);
    }

    private void sendInnerUserData(byte b, byte[] bArr, boolean z, IResultListener<byte[]> iResultListener) {
        sendInnerUserData(b, bArr, 10, z, iResultListener);
    }

    private void sendInnerUserData(final byte b, final byte[] bArr, int i, final boolean z, final IResultListener<byte[]> iResultListener) {
        LogUtils.i(TAG, "sendInnerUserData NOTIMPLEMENTED");
    }

    private synchronized void loopGetTransmissionRate(boolean z) {
        LogUtils.i(TAG, "loopGetTransmissionRate NOTIMPLEMENTED");
    }

    public boolean isMainThread() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            return true;
        }
        return false;
    }

    @Override
    public void changeDefinition(byte b10) {
        changeDefinition(b10, null);
    }

    @Override
    public int getAvBytesPerSec() {
        return nativeGetAvBytesPerSec();
    }

    @Override
    public ConnectMode getConnectMode() {
        int[] nativeGetConnectMode = nativeGetConnectMode();
        ConnectMode connectMode = new ConnectMode();
        if (nativeGetConnectMode != null && nativeGetConnectMode.length == 3) {
            int i = nativeGetConnectMode[0];
            if (i == 0) {
                connectMode.mMode = nativeGetConnectMode[1];
                connectMode.mProtocol = nativeGetConnectMode[2];
            } else {
                connectMode.mMode = i;
                connectMode.mProtocol = -1;
            }
        } else {
            connectMode.mMode = 0;
            connectMode.mProtocol = -1;
        }
        LogUtils.i(TAG, "getConnectMode(): connectMode = " + connectMode.toString());
        return connectMode;
    }

    @Override
    public int getPlayState() {
        return nativeGetPlayState();
    }

    @Override
    public int getVideoHeight() {
        LogUtils.i(TAG, "getVideoHeight nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return -1;
        }
        int playState = getPlayState();
        if (playState != 3 && playState != 4 && playState != 5) {
            return 0;
        }
        AVHeader aVHeader = this.videoSizeMap.get(Byte.valueOf(this.currentDefinition));
        if (aVHeader == null) {
            aVHeader = nativeGetAVHeader();
            this.videoSizeMap.put(Byte.valueOf(this.currentDefinition), aVHeader);
        }
        this.mAVHeader = aVHeader;
        if (aVHeader == null) {
            return 0;
        }
        return aVHeader.getInteger(AVHeader.KEY_HEIGHT, 0);    }

    @Override
    public int getVideoWidth() {
        LogUtils.i(TAG, "getVideoWidth nativeObject:0x" + Long.toHexString(this.nativeObject));
        int playState = getPlayState();
        if (playState != 3 && playState != 4 && playState != 5) {
            return 0;
        }
        AVHeader aVHeader = this.videoSizeMap.get(Byte.valueOf(this.currentDefinition));
        if (aVHeader == null) {
            aVHeader = nativeGetAVHeader();
            this.videoSizeMap.put(Byte.valueOf(this.currentDefinition), aVHeader);
        }
        this.mAVHeader = aVHeader;
        if (aVHeader == null) {
            return 0;
        }
        return aVHeader.getInteger(AVHeader.KEY_WIDTH, 0);
    }

    @Override
    public void initLastFramePath(String str) {
        nativeInitLastFramePath(str);
    }

    @Override
    public void initWmPath(String str) {
        this.wmPath = str;
        LogUtils.i(TAG, "initWmPath:" + this.wmPath);
    }

    @Override
    public boolean isConnectedDevice() {
        int playState = getPlayState();
        if (3 != playState && 4 != playState && 5 != playState && 6 != playState && 8 != playState) {
            return false;
        }
        return true;    }

    @Override
    public boolean isConnectingOrConnectedDev() {
        StackTraceUtils.logStackTrace(TAG, "isConnectingOrConnectedDev");
        int playState = getPlayState();
        return playState == PlayerStateEnum.STATE_IDLE || playState == PlayerStateEnum.STATE_PREPARING;
    }

    @Override
    public boolean isMute() {
        LogUtils.i(TAG, "isMute nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return true;
        }
        return nativeIsMute();
    }

    @Override
    public boolean isPlaying() {
        if (getPlayState() == 5) {
            return true;
        }
        return false;    }

    @Override
    public boolean isRecording() {
        LogUtils.i(TAG, "isRecording nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return false;
        }
        return nativeIsRecording();
    }

    @Override
    public void mute(boolean z10) {
        LogUtils.i(TAG, "mute nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        nativeMute(z10);
    }

    @Override
    public void play() {
        LogUtils.i(TAG, "play nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        if (this.nativeObject == 0) {
            LogUtils.i(TAG, "play failure:nativeObject is invalid, maybe player is released");
        } else if (getPlayState() >= 2 && getPlayState() <= 5) {
            LogUtils.i(TAG, "play failure:preparing or playing, player state:" + getPlayState());
        } else {
            this.mTaskTHandler.removeCallbacksAndMessages(null);
            this.mTaskTHandler.post(new Runnable() {
                @Override
                public void run() {
                    LogUtils.i(TAG, "nativePlay running");
                    nativePlay();
                }
            });
        }
        LogUtils.i(TAG, "play complete state: " + getPlayState());
    }

    @Override
    public void prepare() {
        LogUtils.i(TAG, "prepare nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        if (this.nativeObject == 0) {
            LogUtils.i(TAG, "prepare failure:nativeObject is invalid, maybe player is released");
            return;
        }
        this.mTaskTHandler.removeCallbacksAndMessages(null);
        this.mTaskTHandler.post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer.11
            @Override // java.lang.Runnable
            public void run() {
                nativePrepare();
            }
        });
    }

    @Override
    public void release() {
        LogUtils.i(TAG, "release nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        if (this.nativeObject != 0) {
            nativeRelease();
            this.nativeObject = 0L;
        }
        ConcurrentLinkedQueue<Long> concurrentLinkedQueue = this.mSeekCmdList;
        if (concurrentLinkedQueue != null) {
            concurrentLinkedQueue.clear();
        }
        Handler handler = this.mTaskTHandler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        HandlerThread handlerThread = this.mTaskThread;
        if (handlerThread != null) {
            handlerThread.quit();
        }
        this.mReceiveAvHeader = null;
    }

    public static byte[] addHeader(byte b, byte b2, long j, byte[] bArr) {
        return addHeader(b, b2, (byte) 0, j, bArr);
    }

    private static byte[] addHeader(byte b, byte b2, byte b3, long j, byte[] bArr) {
        byte[] bArr2 = bArr == null ? new byte[8] : new byte[bArr.length + 8];
        bArr2[0] = b;
        bArr2[1] = b2;
        bArr2[2] = (byte) (b3 & 255);
        bArr2[4] = (byte) (j & 255);
        bArr2[5] = (byte) ((j >> 8) & 255);
        bArr2[6] = (byte) ((j >> 16) & 255);
        bArr2[7] = (byte) (255 & (j >> 24));
        if (bArr != null && bArr.length > 0) {
            System.arraycopy(bArr, 0, bArr2, 8, bArr.length);
        }
        LogUtils.i(TAG, "header:" + Arrays.toString(bArr2));
        return bArr2;
    }

    @Override
    public int sendUserData(byte b10, byte[] bArr) {
        LogUtils.i(TAG, "sendUserData cameraId:" + b10 + " nativeObject:" + this.nativeObject);
        if (IoTVideoSdk.isSupportedCurrentAbi()) {
            if (bArr.length > 64512) {
                LogUtils.i(TAG, "sendUserData error due to too large");
                return -3;
            }
            long lowBit32 = ByteUtils.lowBit32(System.currentTimeMillis());
            int nativeSendUserData = nativeSendUserData(addHeader((byte) 1, (byte) 0, b10, lowBit32, bArr), false);
            LogUtils.i(TAG, "sendUserData ret:" + nativeSendUserData + "; timeStamp:" + lowBit32);
            return nativeSendUserData;
        }
        return -1;
    }

    @Override
    public int sendUserData(byte[] bArr) {
        return sendUserData((byte) 0, bArr);
    }

    @Override
    public void setAudioDecoder(IAudioDecoder iAudioDecoder) {
        LogUtils.i(TAG, "setAudioDecoder nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        nativeSetAudioDecoder(iAudioDecoder);
    }

    @Override
    public void setAudioEncoder(IAudioEncoder iAudioEncoder) {
        LogUtils.i(TAG, "setAudioEncoder nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        nativeSetAudioEncoder(iAudioEncoder);
    }

    @Override
    public void setAudioRender(IAudioRender iAudioRender) {
        LogUtils.i(TAG, "setAudioRender(render = " + iAudioRender + "), original mAudioRender = " + this.mAudioRender);
        IAudioRender iAudioRender2 = this.mAudioRender;
        if (iAudioRender2 == iAudioRender) {
            return;
        }
        if (iAudioRender2 != null) {
            iAudioRender2.onRelease();
        }
        this.mAudioRender = iAudioRender;
        if (iAudioRender == null) {
            this.mAudioRenderId = -1L;
            return;
        }
        AVHeader aVHeader = this.mReceiveAvHeader;
        if (aVHeader != null) {
            iAudioRender.onInit(aVHeader);
        }
    }

    @Override
    public void setAvReceiveRateListener(AvReceiveRateListener avReceiveRateListener) {
        this.mReceiveRateListener = avReceiveRateListener;
        loopGetTransmissionRate(isConnectedDevice());
    }

    // https://github.com/GWTimes/IoTVideo-PC/blob/master/%E5%A4%9A%E5%AA%92%E4%BD%93.pdf
    // CONN_TYPE_VIDEO_CALL = 0
    // CONN_TYPE_MONITOR = 1
    // CONN_TYPE_PLAY_REC_FILE = 2
    @Override
    public void setDataResource(String deviceId, int callType, PlayerUserData playerUserData) {
        LogUtils.i(TAG, "setDataResource nativeObject:0x" + Long.toHexString(this.nativeObject));
        LogUtils.i(TAG, "setDataResource deviceId:" + deviceId + "; callType:" + callType + "; userData:" + playerUserData);
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        if (TextUtils.isEmpty(deviceId)) {
            LogUtils.i(TAG, "setDataResource failure:the id of dev is null");
        } else {
            nativeSetDataResource(deviceId, callType, playerUserData);
        }
    }

    @Override
    public void setDisplay(SurfaceHolder surfaceHolder) {
        LogUtils.i(TAG, "setDisplay nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        nativeSetDisplay(surfaceHolder);
    }

    @Override
    public void setErrorListener(IErrorListener iErrorListener) {
        this.mErrorListener = iErrorListener;
    }

    @Override
    public void setOnAVHeaderListener(OnReceiveAVHeaderListener onReceiveAVHeaderListener) {
        this.mOnReceiveAVHeaderListener = onReceiveAVHeaderListener;
    }

    @Override
    public void setOption(int i10, String str, long j10) {

    }

    @Override
    public void setOption(int i10, String str, String str2) {
        Locale locale = Locale.getDefault();
        Object[] objArr = new Object[4];
        objArr[0] = Long.toHexString(this.nativeObject);
        objArr[1] = Integer.valueOf(i10);
        objArr[2] = str == null ? "is null" : str;
        objArr[3] = str2 != null ? str2 : "is null";
        LogUtils.i(TAG, String.format(locale, "setOption, nativeObject:0x%s; category:%d, name:%s; value:%s", objArr));
        if (IoTVideoSdk.isSupportedCurrentAbi()) {
            if (!TextUtils.isEmpty(str) && !TextUtils.isEmpty(str2)) {
                nSetPlayerOption(i10, str, str2);
            } else {
                LogUtils.i(TAG, "setOption failure, input params is invalid");
            }
        }
    }

    @Override
    public void setPlayerVolume(float f10) {
        boolean z;
        StringBuilder sb2 = new StringBuilder("setPlayerVolume audio render is null:");
        if (this.mAudioRender == null) {
            z = true;
        } else {
            z = false;
        }
        sb2.append(z);
        LogUtils.i(TAG, sb2.toString());
        IAudioRender iAudioRender = this.mAudioRender;
        if (iAudioRender != null) {
            iAudioRender.setPlayerVolume(f10);
        }
    }

    @Override
    public void setPreparedListener(IPreparedListener iPreparedListener) {
        this.mPreparedListener = iPreparedListener;
    }

    @Override
    public void setStatusListener(IStatusListener iStatusListener) {
        this.mStatusListener = iStatusListener;
    }

    @Override
    public void setSurface(Surface surface) {
        LogUtils.i(TAG, "setSurface nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        nativeSetSurface(surface);
    }

    @Override
    public void setTimeListener(ITimeListener iTimeListener) {
        this.mTimeListener = iTimeListener;
    }

    @Override
    public void setUserDataListener(IUserDataListener iUserDataListener) {
        this.mUserDataListener = iUserDataListener;
    }

    @Override
    public void setVideoDecoder(IVideoDecoder iVideoDecoder) {
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        nativeSetVideoDecoder(iVideoDecoder);
        LogUtils.i(TAG, "setVideoDecoder nativeObject:0x" + Long.toHexString(this.nativeObject));
    }

    @Override
    public void setVideoEncoder(IVideoEncoder iVideoEncoder) {
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        LogUtils.i(TAG, "setVideoEncoder nativeObject:0x" + Long.toHexString(this.nativeObject));
        nativeSetVideoEncoder(iVideoEncoder);
    }

    @Override
    public void setVideoFluctuationListener(IVideoFluctuationListener iVideoFluctuationListener) {
        setVideoFluctuationListener(iVideoFluctuationListener, new DefaultVideoFluctuationStrategy());
    }

    @Override
    public void setVideoFluctuationListener(IVideoFluctuationListener iVideoFluctuationListener, IVideoFluctuationStrategy iVideoFluctuationStrategy) {
        this.mVideoFluctuationListener = iVideoFluctuationListener;
        this.mVideoFluctuationStrategy = iVideoFluctuationStrategy;
    }

    @Override
    public void setVideoRender(IVideoRender iVideoRender) {
        AVHeader aVHeader;
        LogUtils.i(TAG, "setVideoRender(render = " + iVideoRender + "), original mVideoRender = " + this.mVideoRender);
        IVideoRender iVideoRender2 = this.mVideoRender;
        if (iVideoRender2 == iVideoRender) {
            return;
        }
        if (iVideoRender2 != null) {
            iVideoRender2.onRelease();
        }
        this.mVideoRender = iVideoRender;
        LogUtils.i(TAG, "setVideoRender success");
        if (iVideoRender != null && (aVHeader = this.mReceiveAvHeader) != null) {
            iVideoRender.onInit(aVHeader);
        }
    }

    @Override
    public void setVideoView(IoTVideoView ioTVideoView) {
        LogUtils.i(TAG, "setVideoView(view): nativeObject = 0x" + Long.toHexString(this.nativeObject) + " , view = " + ioTVideoView);
        if (ioTVideoView == null) {
            LogUtils.i(TAG, "setVideoView(view), view is null");
            setAudioRender(null);
            setVideoRender(null);
            return;
        }
        AudioRender audioRender = ioTVideoView.mAudioRender;
        if (audioRender == null) {
            LogUtils.i(TAG, "setVideoView(view), mAudioRender is null");
        } else {
            setAudioRender(audioRender);
            this.mAudioRenderId = -1L;
        }
        GLRenderer gLRenderer = ioTVideoView.mGLRenderer;
        if (gLRenderer == null) {
            LogUtils.i(TAG, "setVideoView(view), mGLRenderer is null");
        } else {
            setVideoRender(gLRenderer);
        }
    }

    @Override
    public void setVideoView(Map<Long, IoTVideoView> map) {
        boolean z;
        IoTVideoView ioTVideoView;
        LogUtils.i(TAG, "setVideoView(viewsMap), viewsMap = " + map);
        if (map != null && map.size() > 0) {
            if (this.mAudioRender == null || (ioTVideoView = map.get(Long.valueOf(this.mAudioRenderId))) == null || ioTVideoView.mAudioRender != this.mAudioRender) {
                z = true;
            } else {
                if (this.mReceiveAvHeader != null) {
                    LogUtils.i(TAG, "setVideoView(viewsMap), mAudioRender.onInit(mReceiveAvHeader)");
                    this.mAudioRender.onInit(this.mReceiveAvHeader);
                }
                z = false;
            }
            if (z) {
                Iterator<Map.Entry<Long, IoTVideoView>> it = map.entrySet().iterator();
                if (it.hasNext()) {
                    Map.Entry<Long, IoTVideoView> next = it.next();
                    LogUtils.i(TAG, "setVideoView(viewsMap), setAudioRender(entry.getValue().mAudioRender)");
                    setAudioRender(next.getValue().mAudioRender);
                    this.mAudioRenderId = next.getKey().longValue();
                }
            }
            HashMap hashMap = new HashMap();
            for (Map.Entry<Long, IoTVideoView> entry : map.entrySet()) {
                hashMap.put(entry.getKey(), entry.getValue().getGLRenderer());
            }
            IVideoRender iVideoRender = this.mVideoRender;
            if (iVideoRender != null && !(iVideoRender instanceof GLRenderGroup)) {
                LogUtils.i(TAG, "setVideoView(viewsMap), mVideoRender.onRelease()");
                if (this.mReceiveAvHeader != null) {
                    this.mVideoRender.onRelease();
                }
                this.mVideoRender = null;
            }
            IVideoRender iVideoRender2 = this.mVideoRender;
            if (iVideoRender2 == null) {
                GLRenderGroup gLRenderGroup = new GLRenderGroup();
                gLRenderGroup.setChildren(hashMap);
                setVideoRender(gLRenderGroup);
                return;
            }
            ((GLRenderGroup) iVideoRender2).update(this.mReceiveAvHeader, hashMap);
            return;
        }
        setAudioRender(null);
        setVideoRender(null);
    }

    @Override
    public void setViewsCreator(IViewsCreator iViewsCreator) {
        LogUtils.i(TAG, "setViewsCreator(..), viewsCreator = " + iViewsCreator);
        this.mViewsCreator = iViewsCreator;
    }

    @Override
    public void snapShot(long j10, String str, ISnapShotListener iSnapShotListener) {
        snapShot(str, 0, iSnapShotListener);
    }

    @Override
    public void snapShot(String str, int i10, ISnapShotListener iSnapShotListener) {
        LogUtils.i(TAG, "snapShot path:" + str);
        snapShot(-1L, str, i10, iSnapShotListener);
    }

    private void snapShot(long j, String str, int i, final ISnapShotListener iSnapShotListener) {
        LogUtils.e(TAG, "snapShot NOT IMPLEMENTED");
    }

    @Override
    public void snapShot(String str, ISnapShotListener iSnapShotListener) {
        LogUtils.e(TAG, "snapShot NOT IMPLEMENTED");
    }

    @Override
    public boolean startRecord(String str, String str2, IRecordListener iRecordListener) {
        return startRecord(str, str2, false, iRecordListener);    }

    @Override
    public boolean startRecord(String str, String str2, boolean z10, IRecordListener iRecordListener) {
        LogUtils.i(TAG, "startRecord NOT IMPLEMENTED");
        return false;
    }

    @Override
    public void stop() {
        LogUtils.i(TAG, "stop nativeObject:0x" + Long.toHexString(this.nativeObject));
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        if (this.nativeObject == 0) {
            LogUtils.i(TAG, "stop failure:nativeObject is invalid");
            return;
        }
        ConcurrentLinkedQueue<Long> concurrentLinkedQueue = this.mSeekCmdList;
        if (concurrentLinkedQueue != null) {
            concurrentLinkedQueue.clear();
        }
        this.mTaskTHandler.removeCallbacksAndMessages(null);
        if (0 != this.nativeObject) {
            nativeStop();
        }
        IVideoFluctuationStrategy iVideoFluctuationStrategy = this.mVideoFluctuationStrategy;
        if (iVideoFluctuationStrategy != null) {
            iVideoFluctuationStrategy.resetStrategy();
        }
    }

    @Override
    public void stopRecord() {
        LogUtils.i(TAG, "stopRecord NOT IMPLEMENTED");
    }

    @Override
    public void updateAccessIdAndToken(long j10, String str) {
        LogUtils.i(TAG, "updateAccessIdAndToken nativeObject:" + this.nativeObject);
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return;
        }
        nativeUpdateAccessIdAndToken(j10, str);
    }
}

