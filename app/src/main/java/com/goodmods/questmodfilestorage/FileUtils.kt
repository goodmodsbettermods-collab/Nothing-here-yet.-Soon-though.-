package com.goodmods.questmodfilestorage

import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {
    fun copy(src: File, dst: File) {
        if (src.isDirectory) {
            if (!dst.exists()) dst.mkdirs()
            val children = src.listFiles() ?: return
            for (c in children) copy(c, File(dst, c.name))
        } else {
            dst.parentFile?.mkdirs()
            src.inputStream().use { input ->
                dst.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun zip(source: File, targetZip: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZip))).use { zos ->
            zipFile(source, source.name, zos)
        }
    }

    private fun zipFile(fileToZip: File, fileName: String, zos: ZipOutputStream) {
        if (fileToZip.isHidden) return
        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles() ?: return
            for (childFile in children) {
                zipFile(childFile, "$fileName/${childFile.name}", zos)
            }
            return
        }
        FileInputStream(fileToZip).use { fis ->
            val zipEntry = ZipEntry(fileName)
            zos.putNextEntry(zipEntry)
            fis.copyTo(zos)
        }
    }

    fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var ze: ZipEntry? = zis.nextEntry
            while (ze != null) {
                val fileName = ze.name
                val newFile = File(targetDir, fileName)
                if (ze.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                ze = zis.nextEntry
            }
        }
    }

    fun md5(file: File): String {
        val buffer = ByteArray(1024 * 4)
        val md = MessageDigest.getInstance("MD5")
        FileInputStream(file).use { fis ->
            var read = fis.read(buffer)
            while (read > 0) {
                md.update(buffer, 0, read)
                read = fis.read(buffer)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
