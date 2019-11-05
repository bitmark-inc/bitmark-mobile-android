/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.converter

import com.bitmark.apiservice.utils.record.AssetRecord
import com.bitmark.apiservice.utils.record.BitmarkRecord
import com.bitmark.apiservice.utils.record.BlockRecord
import com.bitmark.apiservice.utils.record.TransactionRecord
import com.bitmark.registry.data.model.*
import com.bitmark.registry.data.model.entity.AssetDataR
import com.bitmark.registry.data.model.entity.BitmarkDataR
import com.bitmark.registry.data.model.entity.BlockData
import com.bitmark.registry.data.model.entity.TransactionDataR
import javax.inject.Inject

open class Converter @Inject constructor() {

    fun mapBitmark(): (BitmarkRecord) -> BitmarkDataR = { b ->
        mapBitmark(b)
    }

    fun mapBitmark(b: BitmarkRecord) =
        BitmarkDataR(
            b.id,
            b.assetId,
            b.blockNumber,
            b.confirmedAt,
            b.createdAt,
            mapHead(b.head),
            b.headId,
            b.issuedAt,
            b.issuer,
            b.offset,
            b.owner,
            BitmarkData.map(b.status),
            edition = b.edition
        )

    fun mapAsset(): (AssetRecord) -> AssetDataR = { a ->
        mapAsset(a)
    }

    fun mapAsset(a: AssetRecord) =
        AssetDataR(
            a.id,
            a.blockNumber,
            a.blockOffset,
            a.createdAt,
            a.expiredAt,
            a.fingerprint,
            a.metadata,
            a.name,
            a.offset,
            a.registrant,
            AssetData.map(a.status ?: AssetRecord.Status.PENDING)
        )

    fun mapTx(): (TransactionRecord) -> TransactionDataR = { tx ->
        mapTx(tx)
    }

    fun mapTx(tx: TransactionRecord) = TransactionDataR(
        tx.id,
        tx.owner,
        tx.assetId,
        mapHead(tx.head),
        TransactionData.map(tx.status ?: TransactionRecord.Status.PENDING),
        tx.blockNumber,
        tx.blockOffset,
        tx.offset,
        tx.expiredAt,
        tx.payId,
        tx.previousId,
        tx.bitmarkId,
        tx.isCounterSignature,
        tx.previousOwner,
        tx.confirmation
    )

    fun mapBlk(): (BlockRecord) -> BlockData = { b ->
        BlockData(
            b.number,
            b.hash,
            b.bitmarkId,
            b.createdAt
        )
    }
}