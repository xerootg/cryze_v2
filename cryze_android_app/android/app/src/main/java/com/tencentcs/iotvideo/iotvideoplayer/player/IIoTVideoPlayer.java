package com.tencentcs.iotvideo.iotvideoplayer.player;

import android.view.Surface;
import android.view.SurfaceHolder;
import com.tencentcs.iotvideo.iotvideoplayer.AvReceiveRateListener;
import com.tencentcs.iotvideo.iotvideoplayer.ConnectMode;
import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.IErrorListener;
import com.tencentcs.iotvideo.iotvideoplayer.IPreparedListener;
import com.tencentcs.iotvideo.iotvideoplayer.IRecordListener;
import com.tencentcs.iotvideo.iotvideoplayer.ISnapShotListener;
import com.tencentcs.iotvideo.iotvideoplayer.IStatusListener;
import com.tencentcs.iotvideo.iotvideoplayer.ITimeListener;
import com.tencentcs.iotvideo.iotvideoplayer.IUserDataListener;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoFluctuationListener;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoFluctuationStrategy;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoRender;
import com.tencentcs.iotvideo.iotvideoplayer.IViewsCreator;
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoView;
import com.tencentcs.iotvideo.iotvideoplayer.OnReceiveAVHeaderListener;
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioDecoder;
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioEncoder;
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder;
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoEncoder;
import java.util.Map;
/* loaded from: classes2.dex */
public interface IIoTVideoPlayer {
    public static final long INVALID_CAMERA_ID = -1;

    @Deprecated
    void changeDefinition(byte b10);

    int getAvBytesPerSec();

    ConnectMode getConnectMode();

    int getPlayState();

    int getVideoHeight();

    int getVideoWidth();

    void initLastFramePath(String str);

    void initWmPath(String str);

    boolean isConnectedDevice();

    boolean isConnectingOrConnectedDev();

    boolean isMute();

    boolean isPlaying();

    boolean isRecording();

    void mute(boolean z10);

    void play();

    @Deprecated
    void prepare();

    void release();

    int sendUserData(byte b10, byte[] bArr);

    int sendUserData(byte[] bArr);

    void setAudioDecoder(IAudioDecoder iAudioDecoder);

    void setAudioEncoder(IAudioEncoder iAudioEncoder);

    void setAudioRender(IAudioRender iAudioRender);

    void setAvReceiveRateListener(AvReceiveRateListener avReceiveRateListener);

    void setDataResource(String str, int i10, PlayerUserData playerUserData);

    void setDisplay(SurfaceHolder surfaceHolder);

    void setErrorListener(IErrorListener iErrorListener);

    void setOnAVHeaderListener(OnReceiveAVHeaderListener onReceiveAVHeaderListener);

    void setOption(int i10, String str, long j10);

    void setOption(int i10, String str, String str2);

    void setPlayerVolume(float f10);

    void setPreparedListener(IPreparedListener iPreparedListener);

    void setStatusListener(IStatusListener iStatusListener);

    void setSurface(Surface surface);

    void setTimeListener(ITimeListener iTimeListener);

    void setUserDataListener(IUserDataListener iUserDataListener);

    void setVideoDecoder(IVideoDecoder iVideoDecoder);

    void setVideoEncoder(IVideoEncoder iVideoEncoder);

    void setVideoFluctuationListener(IVideoFluctuationListener iVideoFluctuationListener);

    void setVideoFluctuationListener(IVideoFluctuationListener iVideoFluctuationListener, IVideoFluctuationStrategy iVideoFluctuationStrategy);

    void setVideoRender(IVideoRender iVideoRender);

    void setVideoView(IoTVideoView ioTVideoView);

    void setVideoView(Map<Long, IoTVideoView> map);

    void setViewsCreator(IViewsCreator iViewsCreator);

    void snapShot(long j10, String str, ISnapShotListener iSnapShotListener);

    void snapShot(String str, int i10, ISnapShotListener iSnapShotListener);

    void snapShot(String str, ISnapShotListener iSnapShotListener);

    boolean startRecord(String str, String str2, IRecordListener iRecordListener);

    boolean startRecord(String str, String str2, boolean z10, IRecordListener iRecordListener);

    void stop();

    void stopRecord();

    void updateAccessIdAndToken(long j10, String str);
}
