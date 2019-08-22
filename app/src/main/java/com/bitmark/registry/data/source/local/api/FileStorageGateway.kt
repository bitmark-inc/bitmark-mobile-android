package com.bitmark.registry.data.source.local.api

import android.content.Context
import java.io.File


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class FileStorageGateway internal constructor(private val context: Context) {

    fun save(path: String, name: String, data: ByteArray): File {
        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, name)
        if (!file.exists()) file.createNewFile()
        file.writeBytes(data)
        return file
    }

    fun saveOnFilesDir(name: String, data: ByteArray) =
        save(context.filesDir.absolutePath, name, data)

    fun isExistingOnFilesDir(name: String): Boolean =
        File(context.filesDir, name).exists()

    fun isExisting(path: String): Boolean = File(path).exists()

    fun read(path: String): ByteArray? = File(path).readBytes()

    fun readOnFilesDir(name: String): ByteArray? =
        read(File(context.filesDir, name).absolutePath)

    fun filesDir() = context.filesDir

    fun firstFile(path: String): File? {
        val files = listFiles(path)
        return if (files.isNotEmpty()) files[0] else null
    }

    fun listFiles(path: String): List<File> {
        val file = File(path)
        if (!file.exists()) return listOf()
        if (file.isFile) return listOf(file)
        return file.listFiles()?.toList() ?: listOf()
    }

    fun delete(path: String) {
        val file = File(path)
        if (!file.exists()) throw IllegalArgumentException("file does not existed")
        if (file.isDirectory) file.deleteRecursively()
        else file.delete()
    }
}