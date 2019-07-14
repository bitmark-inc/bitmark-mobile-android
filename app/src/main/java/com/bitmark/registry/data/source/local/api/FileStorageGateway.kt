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

    fun save(path: String, data: ByteArray) = File(path).writeBytes(data)

    fun saveOnFilesDir(name: String, data: ByteArray) =
        save(File(context.filesDir, name).absolutePath, data)

    fun isExistingOnFilesDir(name: String): Boolean =
        File(context.filesDir, name).exists()

    fun isExisting(path: String): Boolean = File(path).exists()

    fun read(path: String): ByteArray? = File(path).readBytes()

    fun readOnFilesDir(name: String): ByteArray? =
        read(File(context.filesDir, name).absolutePath)

    fun filesDir() = context.filesDir

}