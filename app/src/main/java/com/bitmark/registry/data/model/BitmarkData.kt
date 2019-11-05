/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.bitmark.apiservice.utils.record.BitmarkRecord
import com.bitmark.registry.data.model.entity.AssetDataR
import com.bitmark.registry.data.model.entity.BitmarkDataL
import com.bitmark.registry.data.model.entity.BitmarkDataR

data class BitmarkData(

    @Embedded
    val bitmarkDataR: BitmarkDataR,

    @Relation(
        parentColumn = "id",
        entityColumn = "bitmark_id",
        entity = BitmarkDataL::class
    )
    val bitmarkDataL: List<BitmarkDataL>,

    @Relation(
        parentColumn = "asset_id",
        entityColumn = "id",
        entity = AssetDataR::class
    )
    var assetData: List<AssetData> = listOf()
) {

    val asset: AssetData?
        get() = if (assetData.isEmpty()) null else assetData[0]

    val id: String
        get() = bitmarkDataR.id

    val assetId: String
        get() = bitmarkDataR.assetId

    val blockNumber: Long
        get() = bitmarkDataR.blockNumber

    val confirmedAt: String?
        get() = bitmarkDataR.confirmedAt

    val createdAt: String?
        get() = bitmarkDataR.createdAt

    val head: Head?
        get() = bitmarkDataR.head

    val headId: String
        get() = bitmarkDataR.headId

    val issuedAt: String?
        get() = bitmarkDataR.issuedAt

    val issuer: String
        get() = bitmarkDataR.issuer

    val offset: Long
        get() = bitmarkDataR.offset

    val owner: String
        get() = bitmarkDataR.owner

    val status: Status
        get() = bitmarkDataR.status

    val edition: Int?
        get() = bitmarkDataR.edition

    val totalEdition: Int?
        get() = bitmarkDataR.totalEdition

    val readableIssuer: String?
        get() = bitmarkDataR.readableIssuer

    val seen: Boolean
        get() = if (bitmarkDataL.isEmpty()) false else bitmarkDataL[0].seen

    fun setAsset(asset: AssetData) {
        this.assetData = listOf(asset)
    }

    enum class Status(val value: String) {
        ISSUING("issuing"),

        TRANSFERRING("transferring"),

        OFFERING("offering"),

        SETTLED("settled"),

        TO_BE_DELETED("to_be_deleted"),

        TO_BE_TRANSFERRED("to_be_transferred");

        companion object {
            fun from(value: String): Status? = when (value) {
                "issuing" -> ISSUING
                "transferring" -> TRANSFERRING
                "offering" -> OFFERING
                "settled" -> SETTLED
                "to_be_deleted" -> TO_BE_DELETED
                "to_be_transferred" -> TO_BE_TRANSFERRED
                else -> null
            }
        }
    }

    companion object {
        fun map(status: BitmarkRecord.Status): Status = when (status) {
            BitmarkRecord.Status.ISSUING -> Status.ISSUING
            BitmarkRecord.Status.OFFERING -> Status.OFFERING
            BitmarkRecord.Status.SETTLED -> Status.SETTLED
            BitmarkRecord.Status.TRANSFERRING -> Status.TRANSFERRING
        }
    }
}