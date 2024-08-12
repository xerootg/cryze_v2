package com.tencentcs.iotvideo.iotvideoplayer;

import java.util.ArrayList;
import java.util.Iterator;

public class DefaultVideoFluctuationStrategy implements IVideoFluctuationStrategy {
    private static final int MAX_SECOND = 4;
    private static final float MIN_PROPORTION = 0.6f;
    private final ArrayList<Float> mFramesProportionList = new ArrayList<>();

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoFluctuationStrategy
    public int calculateStrategy(int i, int i2, int i3) {
        int i4;
        if (i3 <= 0) {
            return -1;
        }
        if (i - i2 < 0) {
            float f = i / i2;
            if (f < MIN_PROPORTION) {
                this.mFramesProportionList.add(Float.valueOf(f));
            } else {
                this.mFramesProportionList.clear();
            }
            if (this.mFramesProportionList.size() > 4) {
                if (i3 == 2) {
                    Iterator<Float> it = this.mFramesProportionList.iterator();
                    float f2 = 0.0f;
                    while (it.hasNext()) {
                        f2 += it.next().floatValue();
                    }
                    if (f2 / 4.0f < 0.3f) {
                        i4 = 0;
                        this.mFramesProportionList.clear();
                        return i4;
                    }
                }
                i4 = i3 - 1;
                this.mFramesProportionList.clear();
                return i4;
            }
        } else {
            this.mFramesProportionList.clear();
        }
        return -1;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoFluctuationStrategy
    public void resetStrategy() {
        this.mFramesProportionList.clear();
    }
}
