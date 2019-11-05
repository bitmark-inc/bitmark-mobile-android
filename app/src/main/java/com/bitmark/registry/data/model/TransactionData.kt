/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.bitmark.apiservice.utils.record.TransactionRecord
import com.bitmark.registry.data.model.entity.AssetDataR
import com.bitmark.registry.data.model.entity.BlockData
import com.bitmark.registry.data.model.entity.TransactionDataR

data class TransactionData(

    @Embedded
    val transactionDataR: TransactionDataR,

    @Relation(
        parentColumn = "asset_id",
        entityColumn = "id",
        entity = AssetDataR::class
    )
    var assetData: List<AssetData> = listOf(),

    @Relation(
        parentColumn = "block_number",
        entityColumn = "number",
        entity = BlockData::class
    )
    var blockData: List<BlockData> = listOf()

) {

    val id: String
        get() = transactionDataR.id

    val owner: String
        get() = transactionDataR.owner

    val assetId: String
        get() = transactionDataR.assetId

    val head: Head?
        get() = transactionDataR.head

    val status: Status
        get() = transactionDataR.status

    val blockNumber: Long
        get() = transactionDataR.blockNumber

    val blockOffset: Long
        get() = transactionDataR.blockOffset

    val offset: Long
        get() = transactionDataR.offset

    val expiresAt: String?
        get() = transactionDataR.expiresAt

    val payId: String
        get() = transactionDataR.payId

    val previousId: String?
        get() = transactionDataR.previousId

    val bitmarkId: String
        get() = transactionDataR.bitmarkId

    val counterSig: Boolean
        get() = transactionDataR.counterSig

    val previousOwner: String?
        get() = transactionDataR.previousOwner

    val confirmation: Int
        get() = transactionDataR.confirmation

    val asset: AssetData?
        get() = if (assetData.isEmpty()) null else assetData[0]

    val block: BlockData?
        get() = if (blockData.isEmpty()) null else blockData[0]

    fun isDeleteTx() = transactionDataR.isDeleteTx()

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

    companion object {
        fun map(status: TransactionRecord.Status): Status = when (status) {
            TransactionRecord.Status.PENDING -> Status.PENDING
            TransactionRecord.Status.CONFIRMED -> Status.CONFIRMED
        }
    }
}