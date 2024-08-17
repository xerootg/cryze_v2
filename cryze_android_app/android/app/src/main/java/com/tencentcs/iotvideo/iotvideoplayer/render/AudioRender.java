package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.media.AudioTrack;
import android.os.Build;
import android.os.Process;
import com.tencentcs.iotvideo.iotvideoplayer.AECManager;
import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.iotvideoplayer.soundtouch.SoundTouchTools;
import com.tencentcs.iotvideo.utils.LogUtils;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/* loaded from: classes2.dex */
public class AudioRender implements IAudioRender {
    private static final boolean DEBUG_AUDIO_PCM_DATA = false;
    private static final int DEFAULT_PLAY_SPEED = 1000;
    private static final int MAX_PROCESS_PLAY_SPEED = 2000;
    private byte[] bytes;
    private int channelLayout;
    private float frameDuration;
    private AVHeader mAudioHeader;
    private AudioTrack mAudioTrack;
    private IoTATimeStampHelper mTimeStampHelper;
    private boolean processAudio;
    private int sampleFormat;
    private int sampleNumPerFrame;
    private int sampleRate;
    private SoundTouchTools soundTouchTools;
    private final String TAG = "AudioRender";
    private boolean doPlayInit = false;
    private FileOutputStream debugOutputStream = null;
    private Lock mAudioTrackLock = new ReentrantLock();
    private float mAudioVolume = 1.0f;
    private long mStartPlayTime = -1;
    private long mReceiveDataDuration = 0;
    private boolean isRecording = false;
    private long mOneSecondDataSize = 0;

    private boolean audioProcess(int i10) {
        if (i10 != DEFAULT_PLAY_SPEED && i10 <= 2000) {
            float f10 = (i10 * 1.0f) / 1000.0f;
            LogUtils.d("AudioRender", "speedParam:" + f10);
            if (this.soundTouchTools == null) {
                LogUtils.i("AudioRender", "audioProcess create SoundTouchTools");
                SoundTouchTools soundTouchTools = new SoundTouchTools();
                this.soundTouchTools = soundTouchTools;
                soundTouchTools.init();
            }
            this.soundTouchTools.clear();
            this.soundTouchTools.setChannels(IoTATimeStampHelper.getChannels(this.mAudioHeader) + 1);
            this.soundTouchTools.setSampleRate(this.sampleRate);
            this.soundTouchTools.setTempoChange(f10);
            return true;
        }
        return false;
    }

    private boolean isNeedAec() {
        if (this.isRecording && AECManager.getInstance().isUsingAEC() && this.mAudioVolume >= 0.0f) {
            return true;
        }
        return false;
    }

    private void openAudioTrack() {
        this.sampleRate = IoTATimeStampHelper.getSampleRate(this.mAudioHeader);
        this.channelLayout = IoTATimeStampHelper.getChannelLayout(this.mAudioHeader);
        this.sampleFormat = IoTATimeStampHelper.getSampleFormat(this.mAudioHeader);
        this.sampleNumPerFrame = IoTATimeStampHelper.getSampleNumPerFrame(this.mAudioHeader);
        this.frameDuration = IoTATimeStampHelper.getFrameDuration(this.mAudioHeader);
        int i10 = this.sampleRate;
        this.mOneSecondDataSize = i10 * 2;
        try {
            int minBufferSize = AudioTrack.getMinBufferSize(i10, this.channelLayout, this.sampleFormat);
            if (Build.MODEL.equals("HTC One X")) {
                this.mAudioTrack = new AudioTrack(0, this.sampleRate, this.channelLayout, this.sampleFormat, minBufferSize, 1);
            } else {
                this.mAudioTrack = new AudioTrack(3, this.sampleRate, this.channelLayout, this.sampleFormat, minBufferSize, 1);
            }
            StringBuilder sb2 = new StringBuilder("sampleRate:");
            sb2.append(this.sampleRate);
            sb2.append("; channelLayout:");
            sb2.append(this.channelLayout);
            sb2.append("; sampleFormat:");
            sb2.append(this.sampleFormat);
            sb2.append("; sampleNumPerFrame:");
            sb2.append(this.sampleNumPerFrame);
            sb2.append("; frameDuration:");
            sb2.append(this.frameDuration);
            sb2.append("; minBufferSize:");
            sb2.append(minBufferSize);
            sb2.append("; mOneSecondDataSize:");
            sb2.append(this.mOneSecondDataSize);
            LogUtils.i("AudioRender", "openAudioTrack:" + ((Object) sb2));
            this.mAudioTrack.play();
            this.mAudioTrack.flush();
        } catch (Exception e10) {
            LogUtils.e("AudioRender", "error: " + e10.getMessage());
        }
    }

    private void playProcessAudio(byte[] bArr, int i10) {
        if (i10 > 0 && i10 <= bArr.length && this.mAudioTrack != null) {
            try {
                if (this.processAudio) {
                    if (i10 % 2 > 0) {
                        LogUtils.e("AudioRender", "playProcessAudio error:data length is odd number");
                        return;
                    }
                    byte[] bArr2 = new byte[i10];
                    int i11 = i10 / 2;
                    short[] sArr = new short[i11];
                    System.arraycopy(bArr, 0, bArr2, 0, i10);
                    ByteBuffer.wrap(bArr2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sArr);
                    byte[] processSamples = this.soundTouchTools.processSamples(sArr, i11);
                    if (processSamples.length > 0) {
                        this.mAudioTrack.write(processSamples, 0, processSamples.length);
                        return;
                    }
                    return;
                }
                if (this.mStartPlayTime < 0) {
                    this.mStartPlayTime = System.currentTimeMillis();
                }
                this.mReceiveDataDuration += (i10 * 1000) / this.mOneSecondDataSize;
                int write = this.mAudioTrack.write(bArr, 0, i10);
                if (write < 0) {
                    LogUtils.e("AudioRender", "playProcessAudio write failure:writeRet=" + write);
                    return;
                }
                return;
            } catch (Exception e10) {
                LogUtils.e("AudioRender", "playProcessAudio exception:" + e10.getMessage() + "; \ncause:" + e10.getCause());
                e10.printStackTrace();
                return;
            }
        }
        LogUtils.e("AudioRender", "playProcessAudio error, audio data length is invalid:" + i10);
    }

    private void saveDebugData(byte[] bArr) {
    }

    private void stopAudioTrack() {
        this.mReceiveDataDuration = 0L;
        this.mStartPlayTime = -1L;
        IoTATimeStampHelper ioTATimeStampHelper = this.mTimeStampHelper;
        if (ioTATimeStampHelper != null) {
            ioTATimeStampHelper.flush();
            this.mTimeStampHelper = null;
        }
        try {
            AudioTrack audioTrack = this.mAudioTrack;
            if (audioTrack != null) {
                if (audioTrack.getState() == 1) {
                    this.mAudioTrack.stop();
                    this.mAudioTrack.release();
                }
                this.mAudioTrack.flush();
                this.mAudioTrack = null;
                this.doPlayInit = false;
            }
        } catch (Exception e10) {
            LogUtils.e("AudioRender", "stopAudioTrack exception:" + e10.getMessage());
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
    public void flushRender() {
        if (this.mAudioTrack == null) {
            LogUtils.i("AudioRender", "flushRender, state: AudioTrack is null");
        } else {
            LogUtils.i("AudioRender", "flushRender, state: " + this.mAudioTrack.getState() + "; rate in AudioTrack:" + this.mAudioTrack.getPlaybackRate());
        }
        this.mAudioTrackLock.lock();
        AudioTrack audioTrack = this.mAudioTrack;
        if (audioTrack != null && (3 == audioTrack.getState() || 1 == this.mAudioTrack.getState())) {
            this.mAudioTrack.flush();
            this.mStartPlayTime = -1L;
            this.mReceiveDataDuration = 0L;
        }
        this.mAudioTrackLock.unlock();
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
    public long getWaitRenderDuration() {
        IoTATimeStampHelper ioTATimeStampHelper;
        AudioTrack audioTrack = this.mAudioTrack;
        if (audioTrack == null || (ioTATimeStampHelper = this.mTimeStampHelper) == null) {
            return 0L;
        }
        long currentPlayPosition = (long) (((float) this.mReceiveDataDuration) - ioTATimeStampHelper.getCurrentPlayPosition(audioTrack));
        if (currentPlayPosition <= 0) {
            return 0L;
        }
        return currentPlayPosition;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
    public void onFrameUpdate(AVData aVData) {
        Object valueOf;
        if (this.mAudioTrackLock.tryLock()) {
            if (this.mAudioTrack != null) {
                if (!this.doPlayInit) {
                    try {
                        Process.setThreadPriority(-19);
                    } catch (Exception e10) {
                        e10.printStackTrace();
                    }
                    this.doPlayInit = true;
                }
                int i10 = aVData.size;
                if (i10 <= 0) {
                    LogUtils.e("AudioRender", "onFrameUpdate failure:the length of AVData is null");
                    return;
                }
                byte[] bArr = this.bytes;
                if (bArr == null || bArr.length < i10) {
                    this.bytes = new byte[i10];
                }
                aVData.data.get(this.bytes, 0, i10);
                saveDebugData(this.bytes);
                if (isNeedAec()) {
                    AECManager.getInstance().render(this.bytes);
                }
                AudioTrack audioTrack = this.mAudioTrack;
                if (audioTrack != null && 1 == audioTrack.getState()) {
                    try {
                        if (!AECManager.getInstance().isUsingAEC() && aVData.size == 0) {
                            this.mAudioTrackLock.unlock();
                            return;
                        }
                        playProcessAudio(this.bytes, aVData.size);
                    } catch (Exception e11) {
                        LogUtils.e("AudioRender", "onFrameUpdate audio track write failure:" + e11.getMessage());
                    }
                } else {
                    StringBuilder sb2 = new StringBuilder("onFrameUpdate failure:audio track is not init,state:");
                    AudioTrack audioTrack2 = this.mAudioTrack;
                    if (audioTrack2 == null) {
                        valueOf = "audio track object is null";
                    } else {
                        valueOf = Integer.valueOf(audioTrack2.getState());
                    }
                    sb2.append(valueOf);
                    LogUtils.e("AudioRender", sb2.toString());
                }
            }
            this.mAudioTrackLock.unlock();
            return;
        }
        LogUtils.e("AudioRender", "onFrameUpdate tryLock failure");
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
    public void onInit(AVHeader aVHeader) {
        LogUtils.i("AudioRender", "onInit " + aVHeader);
        this.mAudioTrackLock.lock();
        this.mAudioHeader = aVHeader;
        if (this.mAudioTrack == null) {
            openAudioTrack();
        }
        this.mTimeStampHelper = new IoTATimeStampHelper(this.mAudioHeader);
        int integer = aVHeader.getInteger(AVHeader.KEY_PLAYBACK_SPEED, DEFAULT_PLAY_SPEED);
        LogUtils.d("AudioRender", "currentSpeed：" + integer);
        this.processAudio = audioProcess(integer);
        this.mAudioTrackLock.unlock();
        LogUtils.i("AudioRender", "onInit end");
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
    public void onRelease() {
        LogUtils.i("AudioRender", "onRelease");
        this.mAudioTrackLock.lock();
        SoundTouchTools soundTouchTools = this.soundTouchTools;
        if (soundTouchTools != null) {
            soundTouchTools.release();
            this.soundTouchTools = null;
        }
        stopAudioTrack();
        this.mAudioTrackLock.unlock();
        LogUtils.i("AudioRender", "onRelease end");
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
    public void setPlayerVolume(float f10) {
        LogUtils.i("AudioRender", "setPlayerVolume:" + f10);
        if (f10 > 1.0f) {
            f10 = 1.0f;
        }
        if (f10 < 0.0f) {
            f10 = 0.0f;
        }
        AudioTrack audioTrack = this.mAudioTrack;
        if (audioTrack != null) {
            this.mAudioVolume = f10;
            audioTrack.setVolume(f10);
        }
    }

    public void setRecordState(boolean z10) {
        LogUtils.i("AudioRender", "setRecordState recording:" + z10);
        this.isRecording = z10;
    }
}
