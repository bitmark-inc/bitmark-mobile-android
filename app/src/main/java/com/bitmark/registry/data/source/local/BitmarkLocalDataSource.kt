package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.ext.isDbRecNotFoundError
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.entity.BlockData
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.model.TransactionData.Status.CONFIRMED
import com.bitmark.registry.data.model.TransactionData.Status.PENDING
import com.bitmark.registry.data.model.entity.*
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

    private var txSavedListener: TxSavedListener? = null

    private var bitmarkSeenListener: BitmarkSeenListener? = null

    private var assetSavedListener: AssetSavedListener? = null

    private var assetTypeChangedListener: AssetTypeChangedListener? = null

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

    fun setTxsSavedListener(listener: TxSavedListener?) {
        this.txSavedListener = listener
    }

    fun setBitmarkSeenListener(listener: BitmarkSeenListener?) {
        this.bitmarkSeenListener = listener
    }

    fun setAssetSavedListener(listener: AssetSavedListener?) {
        this.assetSavedListener = listener
    }

    fun setAssetTypeChangedListener(listener: AssetTypeChangedListener?) {
        this.assetTypeChangedListener = listener
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
    }

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

    fun saveBitmarks(bitmarkRs: List<BitmarkDataR>): Single<MutableList<BitmarkData>> =
        if (bitmarkRs.isEmpty()) {
            Single.just(mutableListOf())
        } else {
            val streams = mutableListOf<Single<BitmarkData>>()
            bitmarkRs.forEach { bitmark -> streams.add(saveBitmark(bitmark)) }
            Single.merge(streams).collectInto(mutableListOf(),
                { t1, t2 -> t1.add(t2) })
        }

    fun saveBitmark(bitmarkR: BitmarkDataR): Single<BitmarkData> =
        checkExistingBitmarkL(bitmarkR.id).flatMapCompletable { existing ->
            databaseApi.rxCompletable { db ->
                val bmLStream = if (existing) {
                    Completable.complete()
                } else {
                    val bitmarkL =
                        BitmarkDataL(
                            bitmarkId = bitmarkR.id
                        )
                    saveBitmarkL(bitmarkL)
                }
                db.bitmarkDao().saveR(bitmarkR).andThen(bmLStream)
            }
        }.andThen(getBitmarkById(bitmarkR.id)).doOnSuccess { bitmark ->
            bitmarkSavedListener?.onBitmarkSaved(
                bitmark
            )
        }

    private fun getBitmarkL(bitmarkId: String) =
        databaseApi.rxSingle { db ->
            db.bitmarkDao()
                .getLByBmId(bitmarkId)
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
                    db.bitmarkDao().deleteLByBmId(bitmarkId)
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
    }.onErrorResumeNext { Single.just(listOf()) }

    fun getBitmarkById(id: String) =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().getById(id)
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

    fun saveAssets(assets: List<AssetDataR>): Single<MutableList<AssetData>> =
        if (assets.isEmpty()) {
            Single.just(mutableListOf())
        } else {
            val streams = mutableListOf<Single<AssetData>>()
            assets.forEach { asset -> streams.add(saveAsset(asset)) }
            Single.merge(streams)
                .collectInto(
                    mutableListOf(),
                    { collection, data -> collection.add(data) })
        }

    fun saveAsset(
        assetR: AssetDataR,
        assetL: AssetDataL = AssetDataL(
            assetId = assetR.id
        )
    ) =
        checkExistingAssetL(assetR.id).flatMap { existing ->
            databaseApi.rxSingle { db ->
                val assetLStream = if (existing) {
                    getAssetL(assetR.id)
                } else {
                    saveAssetL(assetL).andThen(getAssetL(assetR.id))
                }

                db.assetDao().saveR(assetR).andThen(assetLStream)
                    .map { assetL ->
                        AssetData(assetR, listOf(assetL))
                    }
                    .doOnSuccess { asset ->
                        assetSavedListener?.onAssetSaved(
                            asset,
                            !existing
                        )
                    }
            }
        }

    private fun getAssetL(assetId: String) = databaseApi.rxSingle { db ->
        db.assetDao().getLByAssetId(assetId)
    }

    private fun saveAssetL(assetL: AssetDataL) =
        databaseApi.rxCompletable { db ->
            db.assetDao().saveL(assetL)
        }

    private fun checkExistingAssetL(assetId: String) =
        databaseApi.rxSingle { db ->
            db.assetDao().getLByAssetId(assetId)
        }.map { true }.onErrorResumeNext { e ->
            if (e.isDbRecNotFoundError()) {
                Single.just(false)
            } else {
                Single.error(e)
            }
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
    }.doOnSuccess { file ->
        assetFileSavedListener?.onAssetFileSaved(assetId, file)
    }.flatMap { file ->
        val assetType = AssetData.determineAssetType(assetFile = file)
        updateAssetType(assetId, assetType).andThen(Single.just(file))
    }

    private fun updateAssetType(assetId: String, newType: AssetData.Type) =
        getAssetType(assetId).flatMapCompletable { type ->
            if (type != newType && newType != AssetData.Type.UNKNOWN) {
                databaseApi.rxCompletable { db ->
                    db.assetDao().updateTypeByAssetId(assetId, newType)
                }.doOnComplete {
                    assetTypeChangedListener?.onAssetTypeChanged(
                        assetId,
                        newType
                    )
                }
            } else {
                Completable.complete()
            }
        }

    private fun getAssetType(assetId: String) = databaseApi.rxSingle { db ->
        db.assetDao().getTypeByAssetId(assetId)
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

    fun saveTxs(txs: List<TransactionDataR>): Completable {
        return if (txs.isEmpty()) {
            Completable.complete()
        } else {
            val streams = txs.map { tx -> saveTx(tx) }
            Completable.merge(streams)
        }
    }

    private fun saveTx(tx: TransactionDataR) = databaseApi.rxCompletable { db ->
        db.transactionDao()
            .saveR(tx)
    }.andThen(getTxById(tx.id).doOnSuccess { tx -> txSavedListener?.onTxSaved(tx) }).ignoreElement()

    fun listTxs(
        bitmarkId: String,
        isPending: Boolean = false,
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
        }
    }

    // list all txs relevant to the owner. They have been owning or been owned by this owner
    fun listRelevantTxs(
        owner: String,
        offset: Long,
        isPending: Boolean = false,
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
        }
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
        status: TransactionData.Status
    ): Single<List<TransactionData>> =
        databaseApi.rxSingle { db ->
            db.transactionDao().listRelevantByStatusDesc(who, status)
                .onErrorResumeNext {
                    Single.just(
                        listOf()
                    )
                }
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