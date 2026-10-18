package com.goodmods.questmodfilestorage

import java.io.File
import java.security.MessageDigest

object ChecksumUtils {
    fun md5(file: File): String {
        val buffer = ByteArray(1024 * 4)
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { fis ->
            var read = fis.read(buffer)
            while (read > 0) {
                md.update(buffer, 0, read)
                read = fis.read(buffer)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
