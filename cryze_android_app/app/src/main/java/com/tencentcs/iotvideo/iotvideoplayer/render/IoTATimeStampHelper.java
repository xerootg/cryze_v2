package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.media.AudioTimestamp;
import android.media.AudioTrack;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVConstants;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.utils.LogUtils;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/* loaded from: classes2.dex */
public final class IoTATimeStampHelper {
    private static final boolean CALC_BY_HEAD_POSITION = false;
    private static final int POSITION_AVG_ACCOUNT = 10;
    private static final String TAG = "IoTATimeStampHelper";
    private int channelLayout;
    private float frameDuration;
    private int sampleFormat;
    private int sampleNumPerFrame;
    private int sampleRate;
    private float[] headPositionSample = new float[10];
    private int mCurrentSampleIndex = -1;
    private long mGetHeadPositionAccount = 0;
    private float smoothedPlayHeadPosition = 0.0f;
    private Lock mFlushLock = new ReentrantLock();

    public IoTATimeStampHelper(AVHeader aVHeader) {
        this.sampleRate = getSampleRate(aVHeader);
        this.channelLayout = getChannelLayout(aVHeader);
        this.sampleFormat = getSampleFormat(aVHeader);
        this.sampleNumPerFrame = getSampleNumPerFrame(aVHeader);
        this.frameDuration = getFrameDuration(aVHeader);
        StringBuffer stringBuffer = new StringBuffer("sampleRate:");
        stringBuffer.append(this.sampleRate);
        stringBuffer.append("; channelLayout:");
        stringBuffer.append(this.channelLayout);
        stringBuffer.append("; sampleFormat:");
        stringBuffer.append(this.sampleFormat);
        stringBuffer.append("; sampleNumPerFrame:");
        stringBuffer.append(this.sampleNumPerFrame);
        stringBuffer.append("; frameDuration:");
        stringBuffer.append(this.frameDuration);
        LogUtils.i(TAG, "constructor:" + ((Object) stringBuffer));
    }

    public static int getChannelLayout(AVHeader aVHeader) {
        int channels = getChannels(aVHeader);
        if (channels != 0) {
            if (channels != 1) {
                return 16;
            }
            return 12;
        }
        return 4;
    }

    public static int getChannels(AVHeader aVHeader) {
        if (aVHeader == null) {
            return 0;
        }
        return aVHeader.getInteger(AVHeader.KEY_AUDIO_MODE, 0);
    }

    public static float getFrameDuration(AVHeader aVHeader) {
        return (getSampleNumPerFrame(aVHeader) * 1000.0f) / getSampleRate(aVHeader);
    }

    private long getLatency(AudioTrack audioTrack) {
        if (audioTrack == null) {
            LogUtils.e(TAG, "getLatency failure:audioTrack is null");
            return 0L;
        }
        try {
            Object invoke = AudioTrack.class.getMethod("getLatency", null).invoke(audioTrack, new Object[0]);
            if (invoke == null) {
                return 0L;
            }
            return ((Integer) invoke).intValue();
        } catch (Exception e10) {
            LogUtils.e(TAG, "getLatency exception:" + e10.getMessage());
            return 0L;
        }
    }

    private float getPlayPositionByHead(AudioTrack audioTrack) {
        if (audioTrack == null) {
            LogUtils.e(TAG, "getPlayPositionByHead audioTrack is null");
            return 0.0f;
        }
        this.mFlushLock.lock();
        int i10 = this.mCurrentSampleIndex + 1;
        this.mCurrentSampleIndex = i10;
        this.mCurrentSampleIndex = i10 % 10;
        this.headPositionSample[this.mCurrentSampleIndex] = ((audioTrack.getPlaybackHeadPosition() * 1000.0f) / this.sampleRate) - ((float) System.currentTimeMillis());
        long j10 = this.mGetHeadPositionAccount + 1;
        this.mGetHeadPositionAccount = j10;
        long min = Math.min(10L, j10);
        this.smoothedPlayHeadPosition = 0.0f;
        for (int i11 = 0; i11 < min; i11++) {
            this.smoothedPlayHeadPosition += this.headPositionSample[i11];
        }
        this.smoothedPlayHeadPosition = (this.smoothedPlayHeadPosition / ((float) min)) + ((float) System.currentTimeMillis());
        this.mFlushLock.unlock();
        return this.smoothedPlayHeadPosition;
    }

    private float getPlayPositionByTimeStamp(AudioTrack audioTrack) {
        AudioTimestamp audioTimestamp = new AudioTimestamp();
        double playPositionByHead;
        if (audioTrack == null) {
            LogUtils.e(TAG, "getPlayPositionByTimeStamp audioTrack is null");
            return 0.0f;
        }
        if (audioTrack.getTimestamp(new AudioTimestamp())) {
            playPositionByHead = ((System.nanoTime() - audioTimestamp.nanoTime) / 1000000.0d) + ((((float) audioTimestamp.framePosition) * 1000.0f) / this.sampleRate);
        } else {
            playPositionByHead = getPlayPositionByHead(audioTrack);
        }
        return (float) playPositionByHead;
    }

    public static int getSampleFormat(AVHeader aVHeader) {
        if (aVHeader.getInteger(AVHeader.KEY_AUDIO_BIT_WIDTH, 1) != 0) {
            return 2;
        }
        return 3;
    }

    public static int getSampleNumPerFrame(AVHeader aVHeader) {
        return aVHeader.getInteger(AVHeader.KEY_AUDIO_SAMPLE_NUM_PERFRAME, AVConstants.AUDIO_SAMPLE_NUM_1024);
    }

    public static int getSampleRate(AVHeader aVHeader) {
        switch (aVHeader.getInteger(AVHeader.KEY_AUDIO_SAMPLE_RATE, 8000)) {
            case 8000:
            default:
                return 8000;
            case AVConstants.AUDIO_SAMPLE_RATE_11025 /* 11025 */:
                return AVConstants.AUDIO_SAMPLE_RATE_11025;
            case AVConstants.AUDIO_SAMPLE_RATE_12000 /* 12000 */:
                return AVConstants.AUDIO_SAMPLE_RATE_12000;
            case AVConstants.AUDIO_SAMPLE_RATE_16000 /* 16000 */:
                return AVConstants.AUDIO_SAMPLE_RATE_16000;
            case AVConstants.AUDIO_SAMPLE_RATE_22050 /* 22050 */:
                return AVConstants.AUDIO_SAMPLE_RATE_22050;
            case AVConstants.AUDIO_SAMPLE_RATE_24000 /* 24000 */:
                return AVConstants.AUDIO_SAMPLE_RATE_24000;
            case AVConstants.AUDIO_SAMPLE_RATE_32000 /* 32000 */:
                return AVConstants.AUDIO_SAMPLE_RATE_32000;
            case AVConstants.AUDIO_SAMPLE_RATE_44100 /* 44100 */:
                return AVConstants.AUDIO_SAMPLE_RATE_44100;
            case AVConstants.AUDIO_SAMPLE_RATE_48000 /* 48000 */:
                return AVConstants.AUDIO_SAMPLE_RATE_48000;
            case AVConstants.AUDIO_SAMPLE_RATE_64000 /* 64000 */:
                return AVConstants.AUDIO_SAMPLE_RATE_64000;
            case AVConstants.AUDIO_SAMPLE_RATE_96000 /* 96000 */:
                return AVConstants.AUDIO_SAMPLE_RATE_96000;
        }
    }

    public void flush() {
        LogUtils.i(TAG, "flush");
        this.mFlushLock.lock();
        this.mCurrentSampleIndex = -1;
        this.mGetHeadPositionAccount = 0L;
        Arrays.fill(this.headPositionSample, 0.0f);
        this.mFlushLock.unlock();
    }

    public float getCurrentPlayPosition(AudioTrack audioTrack) {
        if (audioTrack == null) {
            LogUtils.e(TAG, "getCurrentPlayPosition audioTrack is null");
            return 0.0f;
        }
        float playPositionByTimeStamp = getPlayPositionByTimeStamp(audioTrack) - ((float) getLatency(audioTrack));
        if (playPositionByTimeStamp < 0.0f) {
            return 0.0f;
        }
        return playPositionByTimeStamp;
    }
}
