package com.tencentcs.iotvideo.iotvideoplayer.mediacodec;

import android.media.MediaCodec;
import androidx.media3.common.MimeTypes;
import com.google.common.base.Ascii;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVConstants;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.utils.ByteUtils;
import java.nio.ByteBuffer;

/* loaded from: classes11.dex */
class MediaConstant {
    public static final int AUDIO_TYPE_G711A = 1;
    public static final int AUDIO_TYPE_G711U = 2;
    public static final int AUDIO_TYPE_PCM = 0;
    public static final int AUDIO_TYPE_PT_AAC = 4;
    public static final int AUDIO_TYPE_PT_ADPCMA = 6;
    public static final int AUDIO_TYPE_PT_AMR = 5;
    public static final int AUDIO_TYPE_PT_G726 = 3;
    public static final int INPUT_BUFFER_ERROR = -1;
    public static final int NAL_UNIT_TYPE_AUD = 9;
    public static final int NAL_UNIT_TYPE_IDR = 5;
    public static final int NAL_UNIT_TYPE_NON_IDR = 1;
    public static final int NAL_UNIT_TYPE_PARTITION_A = 2;
    public static final int NAL_UNIT_TYPE_SPS = 7;
    public static final int SEND_PACKET_ERROR = -11;
    public static final int VIDEO_TYPE_H264 = 1;
    public static final int VIDEO_TYPE_H265 = 5;
    public static final int VIDEO_TYPE_JPEG = 3;
    public static final int VIDEO_TYPE_MJPEG = 4;
    public static final int VIDEO_TYPE_MPEG4 = 2;
    public static final int VIDEO_TYPE_NONE = 0;

    /* loaded from: classes11.dex */
    public enum DecodeState {
        Init,
        Ready,
        Release
    }

    /* loaded from: classes11.dex */
    public static class MediaCodecInputBuffer {
        public ByteBuffer inputBuffer;
        public long presentationTimeUs;
        public int size;

        public MediaCodecInputBuffer(ByteBuffer byteBuffer, int i10, long j10) {
            this.inputBuffer = byteBuffer;
            this.size = i10;
            this.presentationTimeUs = j10;
        }
    }

    /* loaded from: classes11.dex */
    public static class MediaCodecOutputBuffer {
        public int bufferId;
        public MediaCodec.BufferInfo bufferInfo;
        public ByteBuffer outputBuffer;

        public MediaCodecOutputBuffer(ByteBuffer byteBuffer, int i10, MediaCodec.BufferInfo bufferInfo) {
            this.outputBuffer = byteBuffer;
            this.bufferId = i10;
            this.bufferInfo = bufferInfo;
        }
    }

    /* loaded from: classes11.dex */
    public enum MediaType {
        Video,
        Audio,
        Subtitles
    }

    /* loaded from: classes11.dex */
    public interface OnMediaCodecStateChangedListener {
        void onInit(MediaType mediaType);

        void onReady(MediaType mediaType);

        void onRelease(MediaType mediaType);
    }

    MediaConstant() {
    }

    public static ByteBuffer getAacCsd0(AVHeader aVHeader) {
        short sample2MediaCodecIndex = (short) (((aVHeader.getInteger(AVHeader.KEY_AUDIO_MODE, 0) == 0 ? 1 : 2) << 3) | ((short) ((sample2MediaCodecIndex(aVHeader.getInteger(AVHeader.KEY_AUDIO_SAMPLE_RATE, 8000)) << 7) | ((short) 4096))));
        ByteBuffer put = ByteBuffer.allocate(2).put(new byte[]{(byte) ((sample2MediaCodecIndex >> 8) & 255), (byte) (sample2MediaCodecIndex & 255)});
        put.position(0);
        return put;
    }

    public static String getAudioMineByAVHeader(AVHeader aVHeader) throws IllegalArgumentException {
        int integer = aVHeader.getInteger(AVHeader.KEY_AUDIO_TYPE, -1);
        if (integer == 4) {
            return MimeTypes.AUDIO_AAC;
        }
        if (integer == 5) {
            return MimeTypes.AUDIO_AMR_NB;
        }
        throw new IllegalArgumentException("not support this media type");
    }

    public static String getVideoMimeByAVHeader(AVHeader aVHeader) throws IllegalArgumentException {
        int integer = aVHeader.getInteger(AVHeader.KEY_VIDEO_TYPE, -1);
        if (integer == 1) {
            return MimeTypes.VIDEO_H264;
        }
        if (integer == 5) {
            return MimeTypes.VIDEO_H265;
        }
        throw new IllegalArgumentException("not support this media type");
    }

    public static boolean isH264KeyFrame(byte[] bArr) {
        int bytesToInt = ByteUtils.bytesToInt(bArr, 0);
        int i10 = bArr[4] & Ascii.US;
        return bytesToInt == 16777216 && (i10 == 5 || i10 == 7);
    }

    public static int sample2MediaCodecIndex(int i10) {
        switch (i10) {
            case 8000:
            default:
                return 11;
            case 11025:
                return 10;
            case AVConstants.AUDIO_SAMPLE_RATE_12000 /* 12000 */:
                return 9;
            case 16000:
                return 8;
            case AVConstants.AUDIO_SAMPLE_RATE_22050 /* 22050 */:
                return 7;
            case AVConstants.AUDIO_SAMPLE_RATE_24000 /* 24000 */:
                return 6;
            case 32000:
                return 5;
            case AVConstants.AUDIO_SAMPLE_RATE_44100 /* 44100 */:
                return 4;
            case 48000:
                return 3;
            case AVConstants.AUDIO_SAMPLE_RATE_64000 /* 64000 */:
                return 2;
            case 88200:
                return 1;
            case AVConstants.AUDIO_SAMPLE_RATE_96000 /* 96000 */:
                return 0;
        }
    }
}