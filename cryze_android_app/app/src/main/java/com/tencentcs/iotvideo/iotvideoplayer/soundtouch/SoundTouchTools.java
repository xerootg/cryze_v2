package com.tencentcs.iotvideo.iotvideoplayer.soundtouch;

import com.tencentcs.iotvideo.utils.LogUtils;

/* loaded from: classes2.dex */
public class SoundTouchTools {
    static {
        System.loadLibrary("iotsoundtouch");
        LogUtils.i("CLSoundTouchIOT", "Loaded sound touch!");
    }

    public native void clear();

    public native void init();

    public native byte[] processSamples(short[] sArr, int i10);

    @Deprecated
    public native void putSamples(byte[] bArr, int i10);

    @Deprecated
    public native byte[] receiveSamples();

    public native void release();

    public native void setChannels(int i10);

    public native void setPitchSemiTones(float f10);

    public native void setRateChange(float f10);

    public native void setSampleRate(int i10);

    public native void setTempoChange(float f10);
}
