package com.example.myapplication.repository

import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipManager {
    fun unzip(zipInputStream: InputStream, targetDirectory: File) {
        ZipInputStream(zipInputStream).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val file = File(targetDirectory, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile.mkdirs()
                    file.outputStream().use { fileOut ->
                        zip.copyTo(fileOut)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
}

