package com.bitmark.registry.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment.getExternalStorageDirectory
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.bitmark.cryptography.crypto.Random.secureRandomInt
import com.bitmark.cryptography.crypto.Sha3256
import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.bitmark.cryptography.crypto.encoder.Raw.RAW
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder


/**
 * @author Hieu Pham
 * @since 2019-07-30
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MediaUtil {

    companion object {

        private fun isMediaDocument(uri: Uri) =
            "com.android.providers.media.documents" == uri.authority

        private fun isDownloadsDocument(uri: Uri) =
            "com.android.providers.downloads.documents" == uri.authority

        private fun isExternalStorageDocument(uri: Uri) =
            "com.android.externalstorage.documents" == uri.authority

        private fun getDataColumn(
            context: Context, uri: Uri, selection: String?,
            selectionArgs: Array<String>?
        ): String? {

            val projection = arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            try {
                context.contentResolver
                    .query(uri, projection, selection, selectionArgs, null)
                    .use { cursor ->
                        return if (cursor != null && cursor.moveToFirst()) {
                            val columnIndex =
                                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                            cursor.getString(columnIndex)
                        } else null
                    }
            } catch (e: Exception) {
                return null
            }

        }

        private fun getCacheDir(context: Context): File {
            val file = File(context.cacheDir, "media_data")
            if (!file.exists()) file.mkdirs()
            return file
        }

        private fun getRandomFileName() = HEX.encode(
            Sha3256.hash(
                RAW.decode(
                    "%s_%d".format(
                        secureRandomInt(),
                        System.currentTimeMillis()
                    )
                )
            )
        )


        private fun getFileExt(mimeType: String?) =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

        private fun getFileNameFromUri(context: Context, uri: Uri): String {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
                ?: throw IllegalArgumentException("Invalid uri")

            val name = documentFile.name
            val ext = getFileExt(documentFile.type)

            return if (name != null) {
                if (!name.contains(".") || (ext != null && name.contains(".") && !name.substring(
                        name.lastIndexOf(".")
                    ).contains(ext))
                ) "$name.$ext" else name
            } else {
                getRandomFileName()
            }
        }

        private fun writeCacheFile(
            context: Context, uri: Uri, displayName: String
        ): Observable<Progress> = PublishSubject.create<Progress> { emt ->
            try {
                context.contentResolver.openInputStream(uri).use { input ->

                    if (input == null) throw IllegalArgumentException("input stream is null")

                    val prefixFileName = HEX.encode(
                        Sha3256.hash(
                            RAW.decode(
                                uri.authority ?: ""
                                + URLDecoder.decode(uri.path, "UTF-8")
                            )
                        )
                    )

                    val fileName = "%s_%s".format(
                        prefixFileName, displayName.replace(
                            "[$&+,:;=?@#|'<>.^*()%!-/]".toRegex(),
                            "."
                        )
                    )

                    val file = File(getCacheDir(context), fileName)
                    if (!file.exists()) {
                        file.createNewFile()
                        FileOutputStream(file).use { output ->

                            val fileLength = DocumentFile.fromSingleUri(
                                context,
                                uri
                            )?.length() ?: throw IllegalArgumentException(
                                "illegal file length for uri %s".format(
                                    uri.toString()
                                )
                            )

                            var recordedByteLength = 0L
                            val buffer = ByteArray(4 * 1024)
                            var read: Int

                            while (input.read(buffer).also { read = it } >= 0) {
                                output.write(buffer, 0, read)
                                recordedByteLength += buffer.size.toLong()
                                val progress =
                                    (recordedByteLength * 100 / fileLength).toInt()
                                emt.onNext(
                                    Progress(
                                        if (progress >= 100) 100 else progress,
                                        file.absolutePath
                                    )
                                )
                            }
                            output.flush()
                        }
                    }

                    emt.onNext(Progress(100, file.absolutePath))
                    emt.onComplete()
                }
            } catch (e: IOException) {
                emt.onError(e)
            }
        }

        fun getAbsolutePath(
            context: Context,
            uri: Uri
        ): Observable<Progress> =
            try {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    if (isMediaDocument(uri)) {

                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")

                        val contentUri = when (split[0]) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> throw IllegalArgumentException("invalid uri")
                        }

                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])

                        val path = getDataColumn(
                            context,
                            contentUri,
                            selection,
                            selectionArgs
                        )
                        if (path == null) {
                            writeCacheFile(
                                context,
                                contentUri,
                                getFileNameFromUri(context, contentUri)
                            )
                        } else {
                            Observable.just(Progress(path = path))
                        }

                    } else if (isDownloadsDocument(uri)) {

                        val id = DocumentsContract.getDocumentId(uri)

                        if (id.startsWith("raw:")) {

                            Observable.just(
                                Progress(
                                    path = id.replaceFirst(
                                        "raw:",
                                        ""
                                    )
                                )
                            )

                        } else {

                            val contentUriPrefixesToTry = arrayOf(
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads",
                                "content://downloads/all_downloads",
                                "content://com.android.providers.downloads.documents/document"
                            )

                            var path: String? = null
                            for (contentUriPrefix in contentUriPrefixesToTry) {
                                val contentUri = ContentUris.withAppendedId(
                                    Uri.parse(contentUriPrefix), id.toLong()
                                )
                                path = getDataColumn(
                                    context,
                                    contentUri,
                                    null,
                                    null
                                )
                                if (path != null) break
                            }

                            if (path == null) {
                                writeCacheFile(
                                    context,
                                    uri,
                                    getRandomFileName()
                                )
                            } else {
                                Observable.just(Progress(path = path))
                            }
                        }
                    } else if (isExternalStorageDocument(uri)) {

                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        val type = split[0]

                        if ("primary".equals(type, ignoreCase = true)) {
                            Observable.just(
                                Progress(
                                    path = "%s/%s".format(
                                        getExternalStorageDirectory(),
                                        split[1]
                                    )
                                )
                            )

                        } else {

                            val proj = arrayOf(MediaStore.Images.Media.DATA)
                            val cursor = context.contentResolver.query(
                                uri,
                                proj,
                                null,
                                null,
                                null
                            )
                                ?: throw IllegalArgumentException("cursor is null")

                            val columnIndex = cursor
                                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                            cursor.moveToFirst()
                            val path = cursor.getString(columnIndex)
                            cursor.close()
                            Observable.just(Progress(path = path))
                        }
                    } else {
                        writeCacheFile(
                            context,
                            uri,
                            getFileNameFromUri(context, uri)
                        )
                    }
                } else if ("content".equals(
                        uri.scheme,
                        ignoreCase = true
                    )
                ) {

                    val path = getDataColumn(context, uri, null, null)
                    if (path == null)
                        writeCacheFile(
                            context,
                            uri,
                            getFileNameFromUri(context, uri)
                        )
                    else {
                        Observable.just(Progress(path = path))
                    }

                } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                    Observable.just(Progress(path = uri.path))
                } else Observable.empty()

            } catch (e: Throwable) {
                Observable.error<Progress>(e)
            }
    }

    data class Progress(val progress: Int = 100, val path: String?)
}
