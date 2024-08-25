package com.tencentcs.iotvideo.utils

// There's JNI behind this
object Constants {
    val DEV_URL: String = getNetUrl(0)
    val RELEASE_URL: String = getNetUrl(1)
    val THIRD_DEV_URL: String = getNetUrl(2)
    val THIRD_RELEASE_URL: String = getNetUrl(3)
    val CUSTOMER_SYS_DEV_URL: String = getNetUrl(4)
    val CUSTOMER_SYS_RELEASE_URL: String = getNetUrl(5)
    val SAAS_VAS_DEV_URL: String = getNetUrl(6)
    val SAAS_VAS_RELEASE_URL: String = getNetUrl(7)

    external fun getNetUrl(urlType: Int): String
}
