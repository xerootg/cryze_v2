package com.tencentcs.iotvideo

enum class AppLinkState(val value: Int) {
    APP_LINK_ONLINE(1),
    APP_LINK_OFFLINE(2),
    APP_LINK_ACCESS_TOKEN_ERROR(3),
    APP_LINK_TID_INIT_ERROR(4),
    APP_LINK_INVALID_TID(5),
    APP_LINK_KICK_OFF(6),
    APP_LINK_DEV_DISABLE(7),

    /* 8-17? */
    APP_LINK_DEV_REACTIVED(12),
    APP_LINK_TOKEN_EXPIRED(13),
    APP_LINK_TOKEN_DECRYPT_FAIL(14),
    APP_LINK_TOKEN_CHKVAL_ERR(15),
    APP_LINK_TOKEN_CMP_FAIL(16),
    APP_LINK_TERMID_INVALID(17),
    APP_LINK_STATE_UNSET(-1),
    APP_LINK_STATE_ILLEGAL(Int.MIN_VALUE);

    companion object {
        @JvmStatic
        fun fromInt(status: Int): AppLinkState {
            for (state in entries) {
                if (state.value == status) {
                    return state
                }
            }
            return APP_LINK_STATE_ILLEGAL
        }
    }
}


// These are for the native logger which needs additional RE work to use
object LogLevel {
    const val LOG_LEVEL_DEBUG: Int = 5
    const val LOG_LEVEL_ERROR: Int = 2
    const val LOG_LEVEL_FATAL: Int = 1
    const val LOG_LEVEL_INFO: Int = 4
    const val LOG_LEVEL_OFF: Int = 0
    const val LOG_LEVEL_VERBOSE: Int = 6
    const val LOG_LEVEL_WARNING: Int = 3
}
