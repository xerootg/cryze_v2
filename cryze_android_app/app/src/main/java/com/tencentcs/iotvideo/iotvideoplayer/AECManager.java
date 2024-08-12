package com.tencentcs.iotvideo.iotvideoplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVConstants;
import com.tencentcs.iotvideo.utils.ByteUtils;
import com.tencentcs.iotvideo.utils.FileIOUtils;
import com.tencentcs.iotvideo.utils.LogUtils;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/* loaded from: classes2.dex */
public class AECManager {
    public static final int AEC_MODE_DESTROY_AT_RELEASE_IOT_SDK = 1;
    public static final int AEC_MODE_DESTROY_AT_RELEASE_PLAYER = 0;
    private static final String AFTER_AEC_FILE = "after_aec.pcm";
    public static final int AUDIO_SAMPLE_RATE = 8000;
    private static final String BEFORE_AEC_FILE = "before_aec.pcm";
    public static final int CHECK_AEC_GAP = 10000;
    private static final String DEVICE_AUDIO_FILE = "from_camera.pcm";
    public static final int MAX_AEC_DELAY = 50;
    public static final int MAX_AEC_ERROR = 2000;
    private static final int MAX_ALLOW_RENDER_DEVIATION = 200;
    private static final int SUPPORT_AUDIO_BIT_WIDTH = 2;
    private static final int SUPPORT_AUDIO_CHANNELS = 1;
    private static final String TAG = "AECManager";
    private static int mAudio20MsPcmLength;
    private static int mLastAECDelay;
    private static long mLastTimeCheckAECStatus;
    private static long mLastTimeRender;
    private double agcValue;
    private boolean isOpenAecSwitch;
    private boolean isOpenAudioAgc;
    private boolean isUsingAec;
    private int mAecMode;
    private int mAudioAgcValue;
    private int mAudioChannels;
    private int mAudioSample;
    private Context mContext;
    private AECRenderCacheProcessor mRenderCacheProcessor;
    private static final int[] SUPPORT_AUDIO_SAMPLES = {8000, AVConstants.AUDIO_SAMPLE_RATE_16000, AVConstants.AUDIO_SAMPLE_RATE_22050, AVConstants.AUDIO_SAMPLE_RATE_32000, AVConstants.AUDIO_SAMPLE_RATE_44100, AVConstants.AUDIO_SAMPLE_RATE_48000, 88200, AVConstants.AUDIO_SAMPLE_RATE_96000, 192000};
    private static boolean WRITE_PCM_FILE = false;
    private static String PCM_FILE_PATH = "tencentAec";

    /* loaded from: classes2.dex */
    public static class InstanceHolder {
        private static final AECManager INSTANCE = new AECManager();

        private InstanceHolder() {
        }
    }

    private void calculate20msAudioLength(int i10, int i11, int i12) {
        if (i10 > 0 && i11 > 0 && i12 > 0) {
            mAudio20MsPcmLength = (((i10 * i11) * i12) * 20) / 1000;
            String k10 = "calculate20msAudioLength audioSampleRate:" +  i10 +  "; channels:" +  i11 +  "; bitWidth:" + i12 + "; mAudio20MsPcmLength";
            LogUtils.i(TAG, k10.toString() + mAudio20MsPcmLength);
        }
    }

    private void checkAndResetAEC(int i10) {
        boolean z10;
        if (isUsingAEC() && System.currentTimeMillis() - mLastTimeCheckAECStatus > 10000) {
            int aECDelay = getAECDelay();
            int aECStatus = getAECStatus() * 4;
            String g10 = "checkAndResetAEC delay = " + aECDelay +  ", last aec delay = " + mLastAECDelay + ", error = " + aECStatus;
            LogUtils.i(TAG, g10);
            boolean z11 = true;
            if (aECDelay - mLastAECDelay > i10) {
                z10 = true;
            } else {
                z10 = false;
            }
            if (aECStatus <= 2000 && aECStatus >= 0) {
                z11 = false;
            }
            if (!z10 && !z11) {
                mLastAECDelay = aECDelay;
            } else {
                LogUtils.i(TAG, "delay to long and need reset buffer");
                resetAECBuffer();
                mLastAECDelay = getAECDelay();
            }
            mLastTimeCheckAECStatus = System.currentTimeMillis();
        }
    }

    @SuppressLint({"DefaultLocale"})
    private static boolean existAECAarFile(Context context) {
        boolean z10;
        try {
            Class.forName("com.tencent.txtraevoip.txTraeVoip");
            z10 = true;
        } catch (ClassNotFoundException e10) {
            e10.printStackTrace();
            z10 = false;
        }
        LogUtils.i(TAG, "existAECAarFile existedAarFile:" + z10);
        if (z10) {
            return true;
        }
        return false;
    }

    private static String findNativeLibraryPath(Context context, String str) {
        if (context == null || TextUtils.isEmpty(str)) {
            return null;
        }
        return new PathClassLoader(context.getPackageCodePath(), context.getApplicationInfo().nativeLibraryDir, ClassLoader.getSystemClassLoader()).findLibrary(str);
    }

    private int getAECDelay() {
        LogUtils.i(TAG, "getAECDelay exception: NOTIMPLEMENTED");
        return 0;
    }

    private int getAECStatus() {
        LogUtils.i(TAG, "getAECStatus exception: NOTIMPLEMENTED");
        return 0;
    }

    public static AECManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private void pushRenderData() {
        AECRenderCacheProcessor aECRenderCacheProcessor;
        byte[] pullCacheData;
        if (isUsingAEC() && (aECRenderCacheProcessor = this.mRenderCacheProcessor) != null && (pullCacheData = aECRenderCacheProcessor.pullCacheData()) != null && pullCacheData.length != 0) {

            short[] sArr = new short[pullCacheData.length / 2];
            ByteBuffer.wrap(pullCacheData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sArr);
            long currentTimeMillis = System.currentTimeMillis();
            long j10 = mLastTimeRender;
            if (j10 > 0 && currentTimeMillis - j10 > 200) {
                LogUtils.e(TAG, "long time to render, reset the buffer " + (currentTimeMillis - mLastTimeRender));
                resetAECBuffer();
            }
            mLastTimeRender = currentTimeMillis;
        }
    }

    private void resetAECBuffer() {
        if (isUsingAEC()) {
            LogUtils.i(TAG, "resetAECBuffer");
            AECRenderCacheProcessor aECRenderCacheProcessor = this.mRenderCacheProcessor;
            if (aECRenderCacheProcessor != null) {
                aECRenderCacheProcessor.clearCache();
            }
        }
    }

    public int agcPcmData(byte[] bArr, int i10, byte[] bArr2, int i11) {
        if (this.isOpenAudioAgc && 16 == i11) {
            for (int i12 = 0; i12 < i10; i12 += 2) {
                int byte2ToShort = (int) (ByteUtils.byte2ToShort(bArr, i12) * this.agcValue);
                int i13 = 32767;
                if (byte2ToShort <= 32767) {
                    i13 = -32768;
                    if (byte2ToShort >= -32768) {
                        bArr2[i12] = (byte) (byte2ToShort & 255);
                        bArr2[i12 + 1] = (byte) ((byte2ToShort >> 8) & 255);
                    }
                }
                byte2ToShort = i13;
                bArr2[i12] = (byte) (byte2ToShort & 255);
                bArr2[i12 + 1] = (byte) ((byte2ToShort >> 8) & 255);
            }
        }
        return 0;
    }

    public void capture(byte[] bArr) {
        if (bArr != null && bArr.length > 0) {
            if (!hasInitAEC()) {
                LogUtils.e(TAG, "capture failure:has not open aec");
                return;
            } else if (mAudio20MsPcmLength != bArr.length) {
                LogUtils.e(TAG, "capture error:buffer length is illegal, length:" + bArr.length);
                return;
            } else if (isUsingAEC()) {
                pushRenderData();

                short[] sArr = new short[bArr.length / 2];
                ByteBuffer.wrap(bArr).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sArr);
                return;
            } else {
                return;
            }
        }
        LogUtils.e(TAG, "capture failure:invalid input params");
    }

    public void destroyAEC() {
        if (!hasInitAEC()) {
            LogUtils.e(TAG, "destroyAEC failure:has not open aec");
        } else if (this.mAecMode != 0) {
            LogUtils.e(TAG, "destroyAEC failure, aec mode :" + this.mAecMode);
        } else {
            LogUtils.i(TAG, "-----destroyAEC-----");
        }
    }

    public void destroyAECByForcibly() {
        if (!hasInitAEC()) {
            LogUtils.e(TAG, "destroyAECByForcibly failure:has not open aec");
            return;
        }
        LogUtils.i(TAG, "-----destroyAECByForcibly-----");
    }

    public void flushRenderCache() {
        AECRenderCacheProcessor aECRenderCacheProcessor = this.mRenderCacheProcessor;
        if (aECRenderCacheProcessor != null) {
            aECRenderCacheProcessor.clearCache();
        }
    }

    public void get20MsCaptureData(byte[] bArr) {
        if (!hasInitAEC()) {
            LogUtils.e(TAG, "get20MsCaptureData failure:has not open aec");
        } else if (isUsingAEC()) {
            boolean z10 = true;
            checkAndResetAEC(50);
        }
    }

    public int get20msAudioLength() {
        return mAudio20MsPcmLength;
    }

    public boolean hasInitAEC() {
        if (this.isOpenAecSwitch) {
            return true;
        }
        return false;
    }

    public boolean initAEC(int i10, int i11, int i12) {
        boolean z10;
        boolean z11;
        LogUtils.i(TAG, "initAEC audioSampleRate:"+ i10+ "; channels:"+ i11+ "; bitWidth:"+i12);
        if (!this.isOpenAecSwitch) {
            return false;
        }
        if (hasInitAEC() && this.mAudioSample == i10 && this.mAudioChannels == i11) {
            LogUtils.e(TAG, "initAEC :had open aec");
            return true;
        } else if (i10 > 0 && i11 > 0 && i12 > 0) {
            int i13 = 0;
            while (true) {
                int[] iArr = SUPPORT_AUDIO_SAMPLES;
                if (i13 < iArr.length) {
                    if (i10 == iArr[i13]) {
                        z10 = true;
                        break;
                    }
                    i13++;
                } else {
                    z10 = false;
                    break;
                }
            }
            if (!z10) {
                LogUtils.e(TAG, "initAEC failure, illegal sample:" + i10);
                return false;
            } else if (1 != i11) {
                LogUtils.e(TAG, "initAEC failure, illegal channels:" + i11);
                return false;
            } else if (2 != i12) {
                LogUtils.e(TAG, "initAEC failure, illegal bitWidth:" + i12);
                return false;
            } else if (this.mContext == null) {
                return false;
            } else {
                z11 = true;
                if (!z11) {
                    LogUtils.i(TAG, "create traeVoip failure");
                    return false;
                }
                if (z11) {
                    this.mAudioSample = i10;
                    this.mAudioChannels = i11;
                    calculate20msAudioLength(i10, i11, i12);
                }
                if (WRITE_PCM_FILE && this.mContext != null) {
                    LogUtils.i(TAG, "create debug pcm file");
                    mLastTimeRender = 0L;
                    File externalFilesDir = this.mContext.getExternalFilesDir(PCM_FILE_PATH);
                    if (!externalFilesDir.exists()) {
                        WRITE_PCM_FILE &= externalFilesDir.mkdir();
                    }
                    File file = new File(externalFilesDir, DEVICE_AUDIO_FILE);
                    File file2 = new File(externalFilesDir, BEFORE_AEC_FILE);
                    File file3 = new File(externalFilesDir, AFTER_AEC_FILE);
                    if (file.exists()) {
                        WRITE_PCM_FILE &= file.delete();
                    }
                    if (file2.exists()) {
                        WRITE_PCM_FILE &= file2.delete();
                    }
                    if (file3.exists()) {
                        WRITE_PCM_FILE &= file3.delete();
                    }
                }
                return z11;
            }
        } else {
            LogUtils.i(TAG, "initAEC failure:params error!!!");
            return false;
        }
    }

    public boolean isUsingAEC() {
        if (hasInitAEC() && this.isUsingAec) {
            return true;
        }
        return false;
    }

    public void openAudioAgc(boolean z10) {
        this.isOpenAudioAgc = z10;
    }

    public void render(byte[] bArr) {
        if (!hasInitAEC()) {
            LogUtils.e(TAG, "render failure:has not open aec");
            return;
        }
        AECRenderCacheProcessor aECRenderCacheProcessor = this.mRenderCacheProcessor;
        if (aECRenderCacheProcessor != null) {
            aECRenderCacheProcessor.pushCacheData(bArr);
        }
    }

    public void setAecMode(int i10) {
        this.mAecMode = i10;
    }

    public void setAecSwitch(boolean z10, Context context) {
        boolean existAECAarFile = existAECAarFile(context);
        LogUtils.i(TAG, "is exist aec file:" + existAECAarFile);
        if (!existAECAarFile) {
            LogUtils.e(TAG, "Need to import aec aar library files");
        } else if (context == null) {
        } else {
            this.isOpenAecSwitch = z10;
            this.mContext = context;
        }
    }

    public void setAgcValue(int i10) {
        if (!this.isOpenAudioAgc) {
            LogUtils.e(TAG, "setAgcValue failure:the switch of agc is closed");
            return;
        }
        this.mAudioAgcValue = i10;
        this.agcValue = Math.pow(10.0d, i10 / 20.0d);
        String g10 = "setAgcValue agcDbParams:"+ i10+ "; agcValue:" + this.agcValue;
        LogUtils.i(TAG, g10);
    }

    public void startAEC() {
        LogUtils.i(TAG, "-----startAEC-----");
        if (!hasInitAEC()) {
            LogUtils.e(TAG, "startAEC failure:has not open aec");
            return;
        }
        this.isUsingAec = true;
        if (this.mRenderCacheProcessor == null) {
            AECRenderCacheProcessor aECRenderCacheProcessor = new AECRenderCacheProcessor();
            this.mRenderCacheProcessor = aECRenderCacheProcessor;
            aECRenderCacheProcessor.init(mAudio20MsPcmLength);
            LogUtils.i(TAG, "startAEC create render cache processor");
        }
        this.mRenderCacheProcessor.clearCache();
    }

    public void stopAEC() {
        if (!hasInitAEC()) {
            LogUtils.e(TAG, "stopAEC failure:has not open aec");
            return;
        }
        if (getAECDelay() > 0) {
            resetAECBuffer();
        }
        mLastAECDelay = getAECDelay();
        this.isUsingAec = false;
        AECRenderCacheProcessor aECRenderCacheProcessor = this.mRenderCacheProcessor;
        if (aECRenderCacheProcessor != null) {
            aECRenderCacheProcessor.clearCache();
            this.mRenderCacheProcessor.destroy();
            this.mRenderCacheProcessor = null;
        }
        LogUtils.i(TAG, "stopAEC mLastAecDelay= " + mLastAECDelay);
    }

    private AECManager() {
        this.isOpenAecSwitch = false;
        this.isUsingAec = false;
        this.isOpenAudioAgc = false;
        this.mAudioAgcValue = 0;
        this.mAecMode = 0;
        this.mAudioSample = 0;
        this.mAudioChannels = -1;
        mAudio20MsPcmLength = AVConstants.AUDIO_SAMPLE_NUM_320;
        LogUtils.i(TAG, "mAudio20MsPcmLength = " + mAudio20MsPcmLength);
    }
}
