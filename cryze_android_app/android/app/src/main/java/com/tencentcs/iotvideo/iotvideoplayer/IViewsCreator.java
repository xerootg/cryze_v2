package com.tencentcs.iotvideo.iotvideoplayer;

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import java.util.Map;
/* loaded from: classes2.dex */
public interface IViewsCreator {
    Map<Long, IoTVideoView> onCreateViews(AVHeader aVHeader);
}
