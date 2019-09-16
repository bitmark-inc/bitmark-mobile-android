package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.ext.isDbRecNotFoundError
import com.bitmark.registry.data.model.*
import com.bitmark.registry.data.model.TransactionData.Status.CONFIRMED
import com.bitmark.registry.data.model.TransactionData.Status.PENDING
import com.bitmark.registry.data.source.local.api.DatabaseApi
import com.bitmark.registry.data.source.local.api.FileStorageApi
import com.bitmark.registry.data.source.local.api.SharedPrefApi
import com.bitmark.registry.data.source.local.event.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.File
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BitmarkLocalDataSource @Inject constructor(
    databaseApi: DatabaseApi,
    sharedPrefApi: SharedPrefApi, fileStorageApi: FileStorageApi
) : LocalDataSource(databaseApi, sharedPrefApi, fileStorageApi) {

    private var bitmarkDeletedListener: BitmarkDeletedListener? = null

    private var bitmarkStatusChangedListener: BitmarkStatusChangedListener? =
        null

    private var bitmarkSavedListener: BitmarkSavedListener? = null

    private var assetFileSavedListener: AssetFileSavedListener? = null

    private var txsSavedListener: TxsSavedListener? = null

    private var bitmarkSeenListener: BitmarkSeenListener? = null

    private var assetSavedListener: AssetSavedListener? = null

    fun setBitmarkDeletedListener(listener: BitmarkDeletedListener?) {
        this.bitmarkDeletedListener = listener
    }

    fun setBitmarkStatusChangedListener(listener: BitmarkStatusChangedListener?) {
        this.bitmarkStatusChangedListener = listener
    }

    fun setBitmarkSavedListener(listener: BitmarkSavedListener?) {
        this.bitmarkSavedListener = listener
    }

    fun setAssetFileSavedListener(listener: AssetFileSavedListener?) {
        this.assetFileSavedListener = listener
    }

    fun setTxsSavedListener(listener: TxsSavedListener?) {
        this.txsSavedListener = listener
    }

    fun setBitmarkSeenListener(listener: BitmarkSeenListener?) {
        this.bitmarkSeenListener = listener
    }

    fun setAssetSavedListener(listener: AssetSavedListener?) {
        this.assetSavedListener = listener
    }

    //region Bitmark

    fun listBitmarksByOwnerOffsetLimitDesc(
        owner: String,
        offset: Long,
        limit: Int
    ): Single<List<BitmarkData>> = databaseApi.rxSingle { db ->
        db.bitmarkDao().listByOwnerOffsetLimitDesc(owner, offset, limit)
            .onErrorResumeNext {
                Single.just(listOf())
            }
    }.flatMapMaybe(attachAssetToBitmark()).toSingle()

    fun listBitmarksByOwnerStatus(
        owner: String,
        status: BitmarkData.Status
    ): Single<List<BitmarkData>> =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().listByOwnerStatusDesc(owner, status)
                .onErrorResumeNext {
                    Single.just(
                        listOf()
                    )
                }
        }.flatMapMaybe(attachAssetToBitmark()).toSingle()

    private fun attachAssetToBitmark(): (List<BitmarkData>) -> Maybe<List<BitmarkData>> =
        { bitmarks ->

            if (bitmarks.isEmpty()) {
                Maybe.just(bitmarks)
            } else {
                // map asset to bitmark
                val assetStreams = mutableListOf<Maybe<AssetData>>()
                for (bitmark in bitmarks) {
                    assetStreams.add(
                        getAssetById(bitmark.assetId).toMaybe().onErrorResumeNext(
                            Maybe.empty()
                        )
                    )
                }
                Maybe.merge(assetStreams).doOnNext { asset ->
                    bitmarks.filter { b -> b.assetId == asset.id }
                        .forEach { b -> b.asset = asset }
                }.lastOrError().flatMapMaybe { Maybe.just(bitmarks) }
            }
        }

    fun listBitmarksByOwnerStatus(
        owner: String,
        status: List<BitmarkData.Status>
    ): Single<List<BitmarkData>> =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().listByOwnerStatusDesc(owner, status)
                .onErrorResumeNext {
                    Single.just(
                        listOf()
                    )
                }
        }

    fun saveBitmarks(bitmarks: List<BitmarkDataR>): Single<MutableList<BitmarkData>> =
        if (bitmarks.isEmpty()) {
            Single.just(mutableListOf())
        } else {
            val streams = mutableListOf<Single<BitmarkData>>()
            bitmarks.forEach { bitmark -> streams.add(saveBitmark(bitmark)) }
            Single.merge(streams).collectInto(mutableListOf(),
                { t1, t2 -> t1.add(t2) })
        }

    fun saveBitmark(bitmarkR: BitmarkDataR): Single<BitmarkData> =
        checkExistingBitmarkL(bitmarkR.id).flatMap { existing ->
            databaseApi.rxSingle { db ->
                val bmLStream = if (existing) {
                    getBitmarkL(bitmarkR.id)
                } else {
                    val bitmarkL =
                        BitmarkDataL(bitmarkId = bitmarkR.id, seen = false)
                    saveBitmarkL(bitmarkL).andThen(getBitmarkL(bitmarkR.id))
                }
                db.bitmarkDao().saveR(bitmarkR).andThen(bmLStream)
                    .map { bitmarkL -> BitmarkData(bitmarkR, listOf(bitmarkL)) }
            }
        }.flatMap { bitmark ->
            getAssetById(bitmark.bitmarkDataR.assetId).map { asset ->
                bitmark.asset = asset
                bitmark
            }
        }.doOnSuccess { bitmark -> bitmarkSavedListener?.onBitmarkSaved(bitmark) }

    private fun getBitmarkL(bitmarkId: String) =
        databaseApi.rxSingle { db ->
            db.bitmarkDao()
                .getLById(bitmarkId)
        }

    private fun saveBitmarkL(bitmarkL: BitmarkDataL) =
        databaseApi.rxCompletable { db ->
            db.bitmarkDao().saveL(bitmarkL)
        }

    private fun checkExistingBitmarkL(bitmarkId: String) =
        getBitmarkL(bitmarkId).map { true }.onErrorResumeNext { e ->
            if (e.isDbRecNotFoundError()) {
                Single.just(false)
            } else {
                Single.error(e)
            }
        }

    fun updateBitmarkStatus(
        bitmarkId: String,
        status: BitmarkData.Status
    ): Completable =
        databaseApi.rxCompletable { db ->
            db.bitmarkDao().getById(bitmarkId).flatMapCompletable { bitmark ->
                val previousStatus = bitmark.status
                db.bitmarkDao().updateStatus(bitmarkId, status).doOnComplete {
                    bitmarkStatusChangedListener?.onChanged(
                        bitmarkId,
                        previousStatus,
                        status
                    )
                }
            }
        }

    fun deleteBitmarkById(bitmarkId: String) =
        getBitmarkById(bitmarkId).map { b -> b.status }.flatMapCompletable { lastStatus ->
            databaseApi.rxCompletable { db ->
                Completable.mergeArrayDelayError(
                    db.bitmarkDao().deleteRById(bitmarkId),
                    db.bitmarkDao().deleteLById(bitmarkId)
                ).doOnComplete {
                    bitmarkDeletedListener?.onDeleted(
                        bitmarkId,
                        lastStatus
                    )
                }
            }
        }.onErrorResumeNext { e ->
            if (e.isDbRecNotFoundError()) {
                Completable.complete()
            } else {
                Completable.error(e)
            }
        }

    fun maxBitmarkOffset(): Single<Long> =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().maxOffset()
                .onErrorResumeNext { Single.just(-1) }
        }

    fun minBitmarkOffset(status: Array<BitmarkData.Status>): Single<Long> =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().minOffset(status)
                .onErrorResumeNext { Single.just(-1) }
        }

    fun minBitmarkOffset(): Single<Long> =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().minOffset()
                .onErrorResumeNext { Single.just(-1) }
        }

    fun countBitmarks(): Single<Long> = databaseApi.rxSingle { db ->
        db.bitmarkDao().count()
    }.onErrorResumeNext { Single.just(0) }

    fun countUsableBitmarks(owner: String): Single<Long> =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().countUsable(owner)
        }.onErrorResumeNext { Single.just(0) }

    fun markBitmarkSeen(bitmarkId: String): Single<String> =
        databaseApi.rxCompletable { db ->
            db.bitmarkDao().markSeen(bitmarkId)
        }.toSingleDefault(bitmarkId).doOnSuccess {
            bitmarkSeenListener?.onSeen(bitmarkId)
        }

    fun countBitmarkRefSameAsset(assetId: String) = databaseApi.rxSingle { db ->
        db.bitmarkDao().countRefSameAsset(assetId)
    }.onErrorResumeNext { Single.just(0) }

    fun listBitmarkRefSameAsset(assetId: String) = databaseApi.rxSingle { db ->
        db.bitmarkDao().listRefSameAsset(assetId)
    }.onErrorResumeNext { Single.just(listOf()) }.flatMapMaybe(
        attachAssetToBitmark()
    ).toSingle()

    fun getBitmarkById(id: String) =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().getById(id)
        }.flatMap { bitmark ->
            getAssetById(bitmark.assetId).map { asset ->
                bitmark.asset = asset
                bitmark
            }.onErrorResumeNext { Single.just(bitmark) }
        }

    fun checkUnseenBitmark(owner: String) = databaseApi.rxSingle { db ->
        db.bitmarkDao().countUnseen(owner)
    }.onErrorResumeNext { Single.just(0) }.map { count -> count > 0 }

    fun deleteNotOwnBitmarks(owner: String) = databaseApi.rxSingle { db ->
        db.bitmarkDao().listIdNotOwnBy(owner)
    }.flatMapCompletable { ids ->
        if (ids.isEmpty()) {
            Completable.complete()
        } else {
            val streams = mutableListOf<Completable>()
            ids.forEach { id -> streams.add(deleteBitmarkById(id)) }
            Completable.mergeDelayError(streams)
        }
    }

    //endregion Bitmark

    //region Asset

    fun getAssetById(id: String) = databaseApi.rxSingle { db ->
        db.assetDao().getById(id)
    }

    fun deleteAssetById(id: String): Completable =
        databaseApi.rxCompletable { db -> db.assetDao().delete(id) }

    fun saveAssets(assets: List<AssetData>): Completable =
        if (assets.isEmpty()) Completable.complete()
        else {
            val streams = mutableListOf<Completable>()
            assets.forEach { asset -> streams.add(saveAsset(asset)) }
            Completable.mergeDelayError(streams)
        }

    fun saveAsset(asset: AssetData) =
        checkExistingAsset(asset.id).flatMapCompletable { existing ->
            databaseApi.rxCompletable { db ->
                db.assetDao().save(asset).doOnComplete {
                    assetSavedListener?.onAssetSaved(asset, !existing)
                }
            }
        }

    private fun checkExistingAsset(assetId: String) =
        databaseApi.rxSingle { db ->
            db.assetDao().getById(assetId)
        }.map { true }.onErrorResumeNext { e ->
            if (e.isDbRecNotFoundError()) Single.just(
                false
            ) else Single.error(e)
        }

    fun checkAssetFile(
        accountNumber: String,
        assetId: String
    ): Single<Pair<String, File?>> {
        val path = "%s/%s/assets/%s/downloaded".format(
            fileStorageApi.filesDir(),
            accountNumber,
            assetId
        )
        return fileStorageApi.rxSingle { fileGateway ->
            Pair(
                assetId,
                if (fileGateway.isExisting(path)) fileGateway.firstFile(path) else null
            )
        }
    }

    fun saveAssetFile(
        accountNumber: String,
        assetId: String,
        fileName: String,
        content: ByteArray
    ): Single<File> = fileStorageApi.rxSingle { fileGateway ->
        val path = "%s/%s/assets/%s/downloaded".format(
            fileStorageApi.filesDir(),
            accountNumber,
            assetId
        )
        fileGateway.save(path, fileName, content)
    }.doOnSuccess {
        assetFileSavedListener?.onSaved(assetId)
    }

    fun listStoredAssetFile(accountNumber: String) =
        fileStorageApi.rxSingle { fileGateway ->
            val path = "${fileStorageApi.filesDir()}/$accountNumber/assets"
            fileGateway.listFiles(path)
        }

    fun checkRedundantAsset(assetId: String) =
        countBitmarkRefSameAsset(assetId).map { count ->
            count == 0L
        }

    fun deleteAssetFile(accountNumber: String, assetId: String) =
        fileStorageApi.rxCompletable { fileGateway ->
            val path = "%s/%s/assets/%s".format(
                fileStorageApi.filesDir(),
                accountNumber,
                assetId
            )
            fileGateway.delete(path)
        }

    fun deleteAssetFiles(accountNumber: String) =
        fileStorageApi.rxCompletable { fileGateway ->
            val path = "%s/%s/assets".format(
                fileStorageApi.filesDir(),
                accountNumber
            )
            fileGateway.delete(path)
        }

    fun saveEncryptedAssetFile(
        accountNumber: String,
        assetId: String,
        fileName: String,
        content: ByteArray
    ): Single<File> = fileStorageApi.rxSingle { fileGateway ->
        val path = "%s/%s/assets/%s/encrypted".format(
            fileStorageApi.filesDir(),
            accountNumber,
            assetId
        )
        fileGateway.save(path, fileName, content)
    }

    fun deleteEncryptedAssetFile(accountNumber: String, assetId: String) =
        fileStorageApi.rxCompletable { fileGateway ->
            val path = "%s/%s/assets/%s/encrypted".format(
                fileStorageApi.filesDir(),
                accountNumber,
                assetId
            )
            fileGateway.delete(path)
        }

    //endregion Asset

    //region Block

    fun getBlockByNumber(blockNumber: Long): Maybe<BlockData> =
        databaseApi.rxMaybe { db -> db.blockDao().getByNumber(blockNumber) }

    fun saveBlocks(blocks: List<BlockData>): Completable {
        return if (blocks.isEmpty()) Completable.complete()
        else databaseApi.rxCompletable { db -> db.blockDao().save(blocks) }
    }

    //endregion Block

    //region Txs

    fun saveTxs(txs: List<TransactionData>): Completable {
        return if (txs.isEmpty()) Completable.complete()
        else databaseApi.rxCompletable { db ->
            db.transactionDao().save(txs)
        }.andThen(
            Completable.mergeArray(
                attachAssetToTx().invoke(txs).ignoreElement(),
                attachBlkToTx().invoke(txs).ignoreElement()
            ).doOnComplete {
                txsSavedListener?.onTxsSaved(txs)
            }
        )
    }

    fun listTxs(
        bitmarkId: String,
        loadAsset: Boolean = false,
        isPending: Boolean = false,
        loadBlock: Boolean = false,
        limit: Int = 100
    ): Single<List<TransactionData>> {
        val status =
            if (isPending) arrayOf(PENDING, CONFIRMED) else arrayOf(CONFIRMED)
        return databaseApi.rxSingle { db ->
            db.transactionDao()
                .listByBitmarkIdStatusLimitDesc(bitmarkId, status, limit)
                .onErrorResumeNext {
                    Single.just(
                        listOf()
                    )
                }
        }.flatMap { txs ->
            if (loadAsset) attachAssetToTx().invoke(txs) else Single.just(txs)
        }.flatMap { txs ->
            if (loadBlock) attachBlkToTx().invoke(txs) else Single.just(txs)
        }
    }

    // list all txs relevant to the owner. They have been owning or been owned by this owner
    fun listRelevantTxs(
        owner: String,
        offset: Long,
        loadAsset: Boolean = true,
        isPending: Boolean = false,
        loadBlock: Boolean = true,
        limit: Int = 100
    ): Single<List<TransactionData>> {
        val status =
            if (isPending) arrayOf(PENDING, CONFIRMED) else arrayOf(CONFIRMED)
        return databaseApi.rxSingle { db ->
            db.transactionDao()
                .listByOwnerOffsetStatusLimitDesc(
                    owner,
                    owner,
                    offset,
                    status,
                    limit
                )
                .onErrorResumeNext {
                    Single.just(
                        listOf()
                    )
                }
        }.flatMap { txs ->
            if (loadAsset) attachAssetToTx().invoke(txs) else Single.just(txs)
        }.flatMap { txs ->
            if (loadBlock) attachBlkToTx().invoke(txs) else Single.just(txs)
        }
    }

    private fun attachAssetToTx(): (List<TransactionData>) -> Single<List<TransactionData>> =
        { txs ->
            if (txs.isNotEmpty()) {
                val assetIds =
                    txs.distinctBy { tx -> tx.assetId }.map { tx -> tx.assetId }

                val assetStreams = mutableListOf<Maybe<AssetData>>()
                for (id in assetIds) {
                    val assetStream = getAssetById(id).toMaybe()
                        .onErrorResumeNext(Maybe.empty())
                    assetStreams.add(assetStream)
                }

                Maybe.mergeDelayError(assetStreams).doOnNext { asset ->
                    txs.filter { tx -> tx.assetId == asset.id }
                        .forEach { tx -> tx.asset = asset }
                }.collectInto(txs, { _, _ -> }).flatMap { Single.just(txs) }


            } else Single.just(txs)

        }

    private fun attachBlkToTx(): (List<TransactionData>) -> Single<List<TransactionData>> =
        { txs ->
            if (txs.isNotEmpty()) {
                val blockNumbers = txs.distinctBy { tx -> tx.blockNumber }
                    .map { tx -> tx.blockNumber }

                val blockStreams = mutableListOf<Maybe<BlockData>>()
                for (blkNo in blockNumbers) {
                    if (blkNo == 0L) continue
                    val blockStream = getBlockByNumber(blkNo)
                    blockStreams.add(blockStream)
                }

                if (blockStreams.isEmpty()) Single.just(txs)
                else {
                    Maybe.merge(blockStreams).doOnNext { block ->
                        txs.filter { tx ->
                            tx.blockNumber == block.number
                        }.forEach { tx -> tx.block = block }
                    }.collectInto(txs, { _, _ -> }).flatMap { Single.just(txs) }
                }

            } else Single.just(txs)
        }

    fun deleteTxsByBitmarkId(bitmarkId: String) =
        databaseApi.rxCompletable { db ->
            db.transactionDao().deleteByBitmarkId(bitmarkId)
        }

    fun deleteIrrelevantTxsByBitmarkId(who: String, bitmarkId: String) =
        databaseApi.rxCompletable { databaseGateway ->
            databaseGateway.transactionDao()
                .deleteIrrelevantByBitmarkId(who, bitmarkId)
        }

    fun deleteTxsByBitmarkIds(bitmarkIds: List<String>) =
        databaseApi.rxCompletable { db ->
            db.transactionDao().deleteByBitmarkIds(bitmarkIds)
        }

    fun maxRelevantTxOffset(who: String): Single<Long> =
        databaseApi.rxSingle { db ->
            db.transactionDao().maxRelevantOffset(who)
                .onErrorResumeNext { Single.just(-1) }
        }

    fun minRelevantTxOffset(
        who: String,
        status: Array<TransactionData.Status>
    ): Single<Long> =
        databaseApi.rxSingle { db ->
            db.transactionDao().minRelevantOffset(who, status)
                .onErrorResumeNext { Single.just(-1) }
        }

    fun minRelevantTxOffset(who: String): Single<Long> =
        databaseApi.rxSingle { db ->
            db.transactionDao().minRelevantOffset(who)
                .onErrorResumeNext { Single.just(-1) }
        }

    fun listRelevantTxsByStatus(
        who: String,
        status: TransactionData.Status,
        loadAsset: Boolean = true,
        loadBlock: Boolean = true
    ): Single<List<TransactionData>> =
        databaseApi.rxSingle { db ->
            db.transactionDao().listRelevantByStatusDesc(who, status)
                .onErrorResumeNext {
                    Single.just(
                        listOf()
                    )
                }
        }.flatMap { txs ->
            if (loadAsset) attachAssetToTx().invoke(txs) else Single.just(
                txs
            )
        }.flatMap { txs ->
            if (loadBlock) attachBlkToTx().invoke(txs) else Single.just(
                txs
            )
        }

    fun getTxById(id: String) =
        databaseApi.rxSingle { db -> db.transactionDao().getById(id) }

    //endregion Txs

    //region Claim

    fun saveAssetClaimings(assetClaimings: List<AssetClaimingData>) =
        databaseApi.rxCompletable { db ->
            db.assetClaimingDao().save(assetClaimings)
        }

    fun listAssetClaimingRequests(assetId: String, from: String, to: String) =
        databaseApi.rxSingle { db ->
            db.assetClaimingDao().listByAssetIdCreatedAtRange(assetId, from, to)
        }

    //endregion Claim

}