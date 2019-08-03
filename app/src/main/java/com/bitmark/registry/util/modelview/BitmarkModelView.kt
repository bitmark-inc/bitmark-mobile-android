package com.bitmark.registry.util.modelview

import android.os.Parcelable
import android.webkit.MimeTypeMap
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.util.DateTimeUtil
import com.bitmark.registry.util.DateTimeUtil.Companion.OFFICIAL_DATE_FORMAT
import kotlinx.android.parcel.Parcelize
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Parcelize
class BitmarkModelView constructor(
    val id: String,
    val name: String?,
    private val confirmedAt: String?,
    private val createdAt: String? = null,
    val issuer: String,
    val headId: String,
    val metadata: Map<String, String>?,
    val accountNumber: String,
    var seen: Boolean = false,
    var assetType: AssetType = AssetType.UNKNOWN,
    val status: BitmarkData.Status,
    var assetFile: File? = null,
    val assetId: String,
    var previousOwner: String? = null,
    val offset: Long
) : Parcelable {

    companion object {
        fun newInstance(
            bitmark: BitmarkData,
            accountNumber: String
        ): BitmarkModelView {
            val metadata = bitmark.asset?.metadata
            val assetFile = bitmark.asset?.file
            val assetType = determineAssetType(metadata, assetFile)

            return BitmarkModelView(
                id = bitmark.id,
                name = bitmark.asset?.name ?: "",
                confirmedAt = bitmark.confirmedAt,
                createdAt = bitmark.createdAt,
                issuer = bitmark.issuer,
                headId = bitmark.headId,
                metadata = bitmark.asset?.metadata ?: mapOf(),
                accountNumber = accountNumber,
                seen = bitmark.seen,
                assetType = assetType,
                status = bitmark.status,
                assetFile = assetFile,
                assetId = bitmark.assetId,
                offset = bitmark.offset
            )
        }

        fun determineAssetType(
            metadata: Map<String, String>? = null,
            assetFile: File? = null
        ): AssetType {
            if (metadata != null && (metadata.containsKey("source") || metadata.containsKey(
                    "Source"
                ))
            ) {
                return when (metadata["source"] ?: metadata["Source"]) {
                    "Medical Records" -> AssetType.MEDICAL
                    "Health Records" -> AssetType.MEDICAL
                    "Health Kit" -> AssetType.HEALTH
                    "Health" -> AssetType.HEALTH
                    else -> AssetType.UNKNOWN
                }
            } else if (null != assetFile) {
                val mime =
                    MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(assetFile.extension)
                if (null != mime) {
                    return when {
                        mime.contains("image/") -> AssetType.IMAGE
                        mime.contains("video/") -> AssetType.VIDEO
                        mime == "application/zip" || mime == "application/x-7z-compressed" || mime == "application/x-bzip" || mime == "application/x-bzip2" -> AssetType.ZIP
                        mime == "application/msword" || mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || mime == "application/x-abiword" || mime.contains(
                            "application/vnd.oasis.opendocument"
                        ) || mime == "application/pdf" || mime == "application/x-shockwave-flash" || mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> AssetType.DOC

                        else -> AssetType.UNKNOWN
                    }
                } else {
                    return when (assetFile.extension) {
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
                        ) -> AssetType.IMAGE
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
                        ) -> AssetType.VIDEO
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
                        ) -> AssetType.DOC
                        in arrayOf(
                            "7z",
                            "arj",
                            "deb",
                            "pkg",
                            "rar",
                            "rpm",
                            "z",
                            "zip"
                        ) -> AssetType.ZIP
                        else -> AssetType.UNKNOWN
                    }
                }
            }
            return AssetType.UNKNOWN
        }
    }

    enum class AssetType {
        IMAGE, VIDEO, HEALTH, DOC, MEDICAL, ZIP, UNKNOWN
    }

    fun confirmedAt() =
        if (!confirmedAt.isNullOrEmpty()) DateTimeUtil.stringToString(
            confirmedAt
        ) else null

    fun createdAt() =
        if (!createdAt.isNullOrEmpty()) DateTimeUtil.stringToString(
            createdAt,
            OFFICIAL_DATE_FORMAT
        ) else null

    fun isSettled() = status == BitmarkData.Status.SETTLED

    fun isPending() =
        status == BitmarkData.Status.TRANSFERRING || status == BitmarkData.Status.ISSUING
}