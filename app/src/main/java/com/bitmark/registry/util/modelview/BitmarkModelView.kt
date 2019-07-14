package com.bitmark.registry.util.modelview

import android.webkit.MimeTypeMap
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.util.DateTimeUtil


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BitmarkModelView private constructor(
    val name: String?,
    private val confirmedAt: String?,
    val issuer: String,
    val metadata: Map<String, String>?,
    var accountNumber: String,
    var seen: Boolean = false,
    val assetType: AssetType = AssetType.UNKNOWN,
    val status: BitmarkData.Status
) {

    companion object {
        fun newInstance(
            bitmark: BitmarkData, accountNumber: String
        ): BitmarkModelView {
            val metadata = bitmark.asset?.metadata
            var assetType: AssetType = AssetType.UNKNOWN
            val assetFile = bitmark.asset?.file

            if (metadata != null && (metadata.containsKey("source") || metadata.containsKey(
                    "Source"
                ))
            ) {
                assetType = when (metadata["source"] ?: metadata["Source"]) {
                    "Health Records" -> AssetType.MEDICAL
                    "Health Kit" -> AssetType.HEALTH
                    "Health" -> AssetType.HEALTH
                    else -> AssetType.UNKNOWN
                }
            } else if (null != assetFile) {
                val mime =
                    MimeTypeMap.getFileExtensionFromUrl(assetFile.absolutePath)
                if (null != mime) {
                    assetType = when {
                        mime.contains("image/") -> AssetType.IMAGE
                        mime.contains("video/") -> AssetType.VIDEO
                        mime == "application/zip" || mime == "application/x-7z-compressed" || mime == "application/x-bzip" || mime == "application/x-bzip2" -> AssetType.ZIP
                        mime == "application/msword" || mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || mime == "application/x-abiword" || mime.contains(
                            "application/vnd.oasis.opendocument"
                        ) || mime == "application/pdf" || mime == "application/x-shockwave-flash" || mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> AssetType.DOC

                        else -> AssetType.UNKNOWN
                    }
                } else {
                    val ext = assetFile.extension
                    assetType = when (ext) {
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
            return BitmarkModelView(
                bitmark.asset?.name ?: "",
                bitmark.confirmedAt,
                bitmark.issuer,
                bitmark.asset?.metadata ?: mapOf(),
                accountNumber,
                bitmark.seen, assetType, bitmark.status
            )
        }
    }

    enum class AssetType {
        IMAGE, VIDEO, HEALTH, DOC, MEDICAL, ZIP, UNKNOWN
    }

    fun shortIssuer(): String {
        val len = issuer.length
        return String.format(
            "[%s...%s]",
            issuer.substring(0, 4),
            issuer.substring(len - 4, len)
        )
    }

    fun confirmedAt() =
        if (!confirmedAt.isNullOrEmpty()) DateTimeUtil.stringToString(
            confirmedAt
        ) else null
}