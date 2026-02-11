package com.rd.mobile.nfc

import android.nfc.Tag

object NfcUtils {
    fun getTagUidHex(tag: Tag): String {
        val id = tag.id ?: return ""
        val sb = StringBuilder(id.size * 2)
        for (b in id) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}

