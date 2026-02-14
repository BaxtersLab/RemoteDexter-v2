package com.rd.remotedexter.mobile.util

object TimeUtils {
    fun nowUtcSeconds(): Long {
        return System.currentTimeMillis() / 1000L
    }
}

