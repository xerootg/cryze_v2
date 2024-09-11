package com.tencent.mars.xlog

enum class LogLevel() {
    LEVEL_VERBOSE,
    LEVEL_DEBUG,
    LEVEL_INFO,
    LEVEL_WARNING,
    LEVEL_ERROR,
    LEVEL_FATAL,
    LEVEL_NONE;

    fun toXlogLevel(): Int {
        return when (this) {
            LEVEL_VERBOSE -> 0
            LEVEL_DEBUG -> 1
            LEVEL_INFO -> 2
            LEVEL_WARNING -> 3
            LEVEL_ERROR -> 4
            LEVEL_FATAL -> 5
            LEVEL_NONE -> 6
        }
    }

    fun toSdkLevel(): Int {
        return when (this) {
            LEVEL_VERBOSE -> 0
            LEVEL_DEBUG -> 1
            LEVEL_INFO -> 2
            LEVEL_WARNING -> 3
            LEVEL_ERROR -> 4
            LEVEL_FATAL -> 5
            LEVEL_NONE -> 6
        }
    }

    companion object {
        fun fromXlogLevel(level: Int): LogLevel {
            return when (level) {
                0 -> LEVEL_VERBOSE
                1 -> LEVEL_DEBUG
                2 -> LEVEL_INFO
                3 -> LEVEL_WARNING
                4 -> LEVEL_ERROR
                5 -> LEVEL_FATAL
                6 -> LEVEL_NONE
                else -> LEVEL_NONE
            }
        }

        fun fromSdkInt(level: Int): LogLevel {
            return when (level) {
                0 -> LEVEL_VERBOSE
                1 -> LEVEL_DEBUG
                2 -> LEVEL_INFO
                3 -> LEVEL_WARNING
                4 -> LEVEL_ERROR
                5 -> LEVEL_FATAL
                6 -> LEVEL_NONE
                else -> LEVEL_NONE
            }
        }

        fun toXlogLevel(level: Int): LogLevel {
            return when (level) {
                0 -> LEVEL_VERBOSE
                1 -> LEVEL_DEBUG
                2 -> LEVEL_INFO
                3 -> LEVEL_WARNING
                4 -> LEVEL_ERROR
                5 -> LEVEL_FATAL
                6 -> LEVEL_NONE
                else -> LEVEL_NONE
            }
        }

        fun toSdkLevel(level: LogLevel): Int {
            return when (level) {
                LEVEL_VERBOSE -> 0
                LEVEL_DEBUG -> 1
                LEVEL_INFO -> 2
                LEVEL_WARNING -> 3
                LEVEL_ERROR -> 4
                LEVEL_FATAL -> 5
                LEVEL_NONE -> 6
            }
        }
    }
}