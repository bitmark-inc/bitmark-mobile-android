package com.bitmark.registry.data.model

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.bitmark.apiservice.utils.record.AssetRecord
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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
    }

}

