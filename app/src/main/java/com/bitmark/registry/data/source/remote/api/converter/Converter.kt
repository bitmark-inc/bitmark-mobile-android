package com.bitmark.registry.data.source.remote.api.converter

import com.bitmark.apiservice.utils.record.AssetRecord
import com.bitmark.apiservice.utils.record.BitmarkRecord
import com.bitmark.apiservice.utils.record.BlockRecord
import com.bitmark.apiservice.utils.record.TransactionRecord
import com.bitmark.registry.data.model.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
open class Converter @Inject constructor() {

    fun mapBitmark(): (BitmarkRecord) -> BitmarkData = { b ->
        mapBitmark(b)
    }

    fun mapBitmark(b: BitmarkRecord) = BitmarkData(
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
        BitmarkData.map(b.status)
    )

    fun mapAsset(): (AssetRecord) -> AssetData = { a ->
        mapAsset(a)
    }

    fun mapAsset(a: AssetRecord) = AssetData(
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

    fun mapTx(): (TransactionRecord) -> TransactionData = { tx ->
        mapTx(tx)
    }

    fun mapTx(tx: TransactionRecord) = TransactionData(
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
        BlockData(b.number, b.hash, b.bitmarkId, b.createdAt)
    }
}