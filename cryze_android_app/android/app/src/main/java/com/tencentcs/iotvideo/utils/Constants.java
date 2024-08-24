package com.tencentcs.iotvideo.utils;

public class Constants {
    public static final String DEV_URL = Constants.getNetUrl(0);
    public static final String RELEASE_URL = Constants.getNetUrl(1);
    public static final String THIRD_DEV_URL = Constants.getNetUrl(2);
    public static final String THIRD_RELEASE_URL = Constants.getNetUrl(3);
    public static final String CUSTOMER_SYS_DEV_URL = Constants.getNetUrl(4);
    public static final String CUSTOMER_SYS_RELEASE_URL = Constants.getNetUrl(5);
    public static final String SAAS_VAS_DEV_URL = Constants.getNetUrl(6);
    public static final String SAAS_VAS_RELEASE_URL = Constants.getNetUrl(7);

    public static native String getNetUrl(int urlType);
}
