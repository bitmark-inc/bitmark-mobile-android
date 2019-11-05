/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.model

import android.webkit.MimeTypeMap
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.bitmark.apiservice.utils.record.AssetRecord
import com.bitmark.registry.data.model.entity.AssetDataL
import com.bitmark.registry.data.model.entity.AssetDataR
import java.io.File

data class AssetData(
    @Embedded
    val assetDataR: AssetDataR,

    @Relation(
        parentColumn = "id",
        entityColumn = "asset_id",
        entity = AssetDataL::class
    )
    val assetDataL: List<AssetDataL> = listOf(
        AssetDataL(
            assetId = assetDataR.id,
            type = Type.UNKNOWN
        )
    )
) {

    @Ignore
    var file: File? = null

    val id: String
        get() = assetDataR.id

    val blockNumber: Long
        get() = assetDataR.blockNumber

    val blockOffset: Long
        get() = assetDataR.blockOffset

    val createdAt: String?
        get() = assetDataR.createdAt

    val expiresAt: String?
        get() = assetDataR.expiresAt

    val fingerprint: String
        get() = assetDataR.fingerprint

    val metadata: Map<String, String>?
        get() = assetDataR.metadata

    val name: String?
        get() = assetDataR.name

    val offset: Long
        get() = assetDataR.offset

    val registrant: String
        get() = assetDataR.registrant

    val status: Status
        get() = assetDataR.status

    val type: Type
        get() = if (assetDataL.isEmpty()) Type.UNKNOWN else assetDataL[0].type

    enum class Status(val value: String) {
        CONFIRMED("confirmed"),

        PENDING("pending");

        companion object {
            fun from(value: String): Status? = when (value) {
                "confirmed" -> CONFIRMED
                "pending" -> PENDING
                else -> null
            }
        }
    }

    enum class Type(val value: String) {
        IMAGE("image"),

        VIDEO("video"),

        HEALTH("health"),

        DOC("doc"),

        MEDICAL("medical"),

        ZIP("zip"),

        UNKNOWN("unknown");

        companion object {
            fun from(value: String): Type = when (value) {
                "image" -> IMAGE
                "video" -> VIDEO
                "health" -> HEALTH
                "doc" -> DOC
                "medical" -> MEDICAL
                "zip" -> ZIP
                else -> UNKNOWN
            }
        }
    }

    companion object {
        fun map(status: AssetRecord.Status): Status = when (status) {
            AssetRecord.Status.PENDING -> Status.PENDING
            AssetRecord.Status.CONFIRMED -> Status.CONFIRMED
        }

        fun determineAssetTypeByMetadata(metadata: Map<String, String>): Type? {
            return when (metadata["source"] ?: metadata["Source"]) {
                "Medical Records", "Health Records" -> Type.MEDICAL
                "Health Kit", "Health" -> Type.HEALTH
                // TODO consider to add it since maybe it's inconsistent
                /*"Photo", "photo" -> AssetType.IMAGE
                "Video", "video" -> AssetType.VIDEO*/
                else -> null
            }
        }

        fun determineAssetTypeByExt(ext: String): Type? {
            return when (ext) {
                in arrayOf(
                    "ai",
                    "bmp",
                    "gif",
                    "ico",
                    "jpeg",
                    "jpg",
                    "png",
                    "ps",
                    "psd",
                    "svg",
                    "tif",
                    "tiff"
                ) -> Type.IMAGE
                in arrayOf(
                    "3g2",
                    "3gp",
                    "avi",
                    "flv",
                    "h264",
                    "m4v",
                    "mkv",
                    "mov",
                    "mp4",
                    "mpg",
                    "mpeg",
                    "rm",
                    "swf",
                    "vob",
                    "wmv"
                ) -> Type.VIDEO
                in arrayOf(
                    "doc",
                    "docx",
                    "pdf",
                    "rtf",
                    "tex",
                    "txt",
                    "wks",
                    "wps",
                    "wpd"
                ) -> Type.DOC
                in arrayOf(
                    "7z",
                    "arj",
                    "deb",
                    "pkg",
                    "rar",
                    "rpm",
                    "z",
                    "zip"
                ) -> Type.ZIP
                else -> null
            }
        }

        fun determineAssetTypeByMime(mime: String): Type? {
            return when {
                mime.contains("image/") -> Type.IMAGE
                mime.contains("video/") -> Type.VIDEO
                mime == "application/zip" || mime == "application/x-7z-compressed" || mime == "application/x-bzip" || mime == "application/x-bzip2" -> Type.ZIP
                mime == "application/msword" || mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || mime == "application/x-abiword" || mime.contains(
                    "application/vnd.oasis.opendocument"
                ) || mime == "application/pdf" || mime == "application/x-shockwave-flash" || mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> Type.DOC

                else -> null
            }
        }

        fun determineAssetType(
            metadata: Map<String, String>? = null,
            assetFile: File? = null
        ): Type {
            var assetType: Type? = null
            if (metadata != null) {
                assetType = determineAssetTypeByMetadata(metadata)
            }

            if (assetType != null) return assetType
            val mime =
                MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(assetFile?.extension)
            if (mime != null) {
                assetType = determineAssetTypeByMime(mime)
            }

            if (assetType != null) return assetType
            val ext = assetFile?.extension
            if (ext != null) {
                assetType = determineAssetTypeByExt(ext)
            }

            return assetType ?: Type.UNKNOWN
        }
    }

}

