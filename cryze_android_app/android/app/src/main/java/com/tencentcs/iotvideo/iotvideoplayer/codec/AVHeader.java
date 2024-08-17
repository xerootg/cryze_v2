package com.tencentcs.iotvideo.iotvideoplayer.codec;

import com.tencentcs.iotvideo.utils.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
/* loaded from: classes2.dex */
public class AVHeader {
    public static final String KEY_AUDIO_BIT_WIDTH = "audio-bit-width";
    public static final String KEY_AUDIO_CODEC_OPTION = "audio-codec-option";
    public static final String KEY_AUDIO_ENCODER_LIB_ID = "audio-encoder-lib-id";
    public static final String KEY_AUDIO_MODE = "audio-mode";
    public static final String KEY_AUDIO_SAMPLE_NUM_PERFRAME = "audio-sample-num-perframe";
    public static final String KEY_AUDIO_SAMPLE_RATE = "audio-sample-rate";
    public static final String KEY_AUDIO_TYPE = "audio-type";
    public static final String KEY_BIT_RATE = "bitrate";
    public static final String KEY_FRAME_RATE = "frame-rate";
    public static final String KEY_HEIGHT = "height";
    public static final String KEY_PLAYBACK_SPEED = "playback-speed";
    public static final String KEY_VIDEO_RENDER_INFO = "videoRenderInfo";
    public static final String KEY_VIDEO_TYPE = "video-type";
    public static final String KEY_WIDTH = "width";
    private static final String TAG = "AVHeader";
    public Map<String, Object> map = new HashMap();

    private final int getInteger(String str) {
        return ((Integer) this.map.get(str)).intValue();
    }

    public final boolean containsKey(String str) {
        return this.map.containsKey(str);
    }

    public final void copy(AVHeader aVHeader) {
        if (aVHeader == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : aVHeader.map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                if (value instanceof Integer) {
                    setInteger(key, ((Integer) entry.getValue()).intValue());
                } else if (value instanceof String) {
                    setString(key, (String) entry.getValue());
                } else if (value instanceof VideoRenderInfo) {
                    setVideoRenderInfo(((VideoRenderInfo) value).copy());
                }
            }
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            return Objects.equals(this.map, ((AVHeader) obj).map);
        }
        return false;
    }

    public final String getString(String str) {
        return (String) this.map.get(str);
    }

    public VideoRenderInfo getVideoRenderInfo() {
        try {
            return (VideoRenderInfo) this.map.get(KEY_VIDEO_RENDER_INFO);
        } catch (Exception unused) {
            return null;
        }
    }

    public final void setInteger(String str, int i10) {
        this.map.put(str, Integer.valueOf(i10));
    }

    public final void setString(String str, String str2) {
        this.map.put(str, str2);
    }

    public void setVideoRenderInfo(VideoRenderInfo videoRenderInfo) {
        LogUtils.d(TAG, "setVideoRenderInfo, videoRenderInfo = " + videoRenderInfo);
        this.map.put(KEY_VIDEO_RENDER_INFO, videoRenderInfo);
    }

    public String toString() {
        return "AVHeader{map=" + this.map + '}';
    }

    public final int getInteger(String str, int defaultValue) {
        try {
            return getInteger(str);
        } catch (NullPointerException unused) {
            return defaultValue;
        }
    }

    public final String getString(String str, String str2) {
        String string = getString(str);
        return string == null ? str2 : string;
    }
}
