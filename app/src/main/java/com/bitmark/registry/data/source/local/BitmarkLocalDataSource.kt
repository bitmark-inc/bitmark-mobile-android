package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.BlockData
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.model.TransactionData.Status.CONFIRMED
import com.bitmark.registry.data.model.TransactionData.Status.PENDING
import com.bitmark.registry.data.source.local.api.DatabaseApi
import com.bitmark.registry.data.source.local.api.FileStorageApi
import com.bitmark.registry.data.source.local.api.SharedPrefApi
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

    private var bitmarkInsertedListener: BitmarkInsertedListener? = null

    private var assetFileSavedListener: AssetFileSavedListener? = null

    fun setBitmarkDeletedListener(listener: BitmarkDeletedListener?) {
        this.bitmarkDeletedListener = listener
    }

    fun setBitmarkStatusChangedListener(listener: BitmarkStatusChangedListener?) {
        this.bitmarkStatusChangedListener = listener
    }

    fun setBitmarkInsertedListener(listener: BitmarkInsertedListener?) {
        this.bitmarkInsertedListener = listener
    }

    fun setAssetFileSavedListener(listener: AssetFileSavedListener?) {
        this.assetFileSavedListener = listener
    }

    //region Bitmark

    fun listBitmarksByOwnerOffsetLimitDesc(
        owner: String,
        offset: Long,
        limit: Int
    ): Maybe<List<BitmarkData>> = databaseApi.rxMaybe { db ->
        db.bitmarkDao().listByOwnerOffsetLimitDesc(owner, offset, limit)
    }

    fun listBitmarksByOwnerStatus(
        owner: String,
        status: BitmarkData.Status
    ): Maybe<List<BitmarkData>> =
        databaseApi.rxMaybe { db ->
            db.bitmarkDao().listByOwnerStatusDesc(owner, status)
        }

    fun listBitmarksByOwnerStatus(
        owner: String,
        status: List<BitmarkData.Status>
    ): Maybe<List<BitmarkData>> =
        databaseApi.rxMaybe { db ->
            db.bitmarkDao().listByOwnerStatusDesc(owner, status)
        }

    fun saveBitmarks(bitmarks: List<BitmarkData>): Completable =
        if (bitmarks.isEmpty()) Completable.complete()
        else databaseApi.rxCompletable { db ->
            db.bitmarkDao().save(bitmarks).doOnComplete {
                val insertedIds = bitmarks.map { b -> b.id }
                bitmarkInsertedListener?.onInserted(insertedIds)
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

    fun deleteBitmarkById(bitmarkId: String) = databaseApi.rxCompletable { db ->
        db.bitmarkDao().deleteById(bitmarkId).doOnComplete {
            bitmarkDeletedListener?.onDeleted(listOf(bitmarkId))
        }
    }

    fun deleteBitmarkByIds(bitmarkIds: List<String>) =
        databaseApi.rxCompletable { db ->
            db.bitmarkDao().deleteByIds(bitmarkIds).doOnComplete {
                bitmarkDeletedListener?.onDeleted(bitmarkIds)
            }
        }

    fun maxBitmarkOffset(): Single<Long> =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().maxOffset()
                .onErrorResumeNext { Single.just(-1) }
        }

    fun countBitmarks(): Single<Long> = databaseApi.rxSingle { db ->
        db.bitmarkDao().count()
    }.onErrorResumeNext { Single.just(0) }

    fun countUsableBitmarks(owner: String): Single<Long> =
        databaseApi.rxSingle { db ->
            db.bitmarkDao().countUsableBitmarks(owner)
        }.onErrorResumeNext { Single.just(0) }

    fun markBitmarkSeen(bitmarkId: String): Single<String> =
        databaseApi.rxCompletable { db ->
            db.bitmarkDao().markSeen(bitmarkId)
        }.toSingleDefault(bitmarkId)

    fun countBitmarkRefSameAsset(assetId: String) = databaseApi.rxSingle { db ->
        db.bitmarkDao().countBitmarkRefSameAsset(assetId)
    }.onErrorResumeNext { Single.just(0) }

    fun listBitmarkRefSameAsset(assetId: String) = databaseApi.rxSingle { db ->
        db.bitmarkDao().listBitmarkRefSameAsset(assetId)
    }.onErrorResumeNext { Single.just(listOf()) }

    fun getBitmarkById(id: String) =
        databaseApi.rxMaybe { db -> db.bitmarkDao().getById(id) }

    //endregion Bitmark

    //region Asset

    fun getAssetById(id: String): Maybe<AssetData> = databaseApi.rxMaybe { db ->
        db.assetDao().getById(id)
    }

    fun deleteAssetById(id: String): Completable =
        databaseApi.rxCompletable { db -> db.assetDao().delete(id) }

    fun saveAssets(assets: List<AssetData>): Completable =
        if (assets.isEmpty()) Completable.complete()
        else databaseApi.rxCompletable { db ->
            db.assetDao().save(assets)
        }

    fun checkAssetFile(
        accountNumber: String,
        assetId: String
    ): Single<Pair<String, File?>> {
        val path = String.format(
            "%s/%s/assets/%s/downloaded",
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
        val path = String.format(
            "%s/%s/assets/%s/downloaded",
            fileStorageApi.filesDir(),
            accountNumber,
            assetId
        )
        fileGateway.save(path, fileName, content)
    }.doOnSuccess {
        assetFileSavedListener?.onSaved(assetId)
    }

    fun checkRedundantAsset(assetId: String) =
        countBitmarkRefSameAsset(assetId).map { count ->
            count == 0L
        }

    fun deleteAssetFile(accountNumber: String, assetId: String) =
        fileStorageApi.rxCompletable { fileGateway ->
            val path = String.format(
                "%s/%s/assets/%s",
                fileStorageApi.filesDir(),
                accountNumber,
                assetId
            )
            fileGateway.delete(path)
        }.onErrorResumeNext { Completable.complete() }

    fun saveEncryptedAssetFile(
        accountNumber: String,
        assetId: String,
        fileName: String,
        content: ByteArray
    ): Single<File> = fileStorageApi.rxSingle { fileGateway ->
        val path = String.format(
            "%s/%s/assets/%s/encrypted",
            fileStorageApi.filesDir(),
            accountNumber,
            assetId
        )
        fileGateway.save(path, fileName, content)
    }

    fun deleteEncryptedAssetFile(accountNumber: String, assetId: String) =
        fileStorageApi.rxCompletable { fileGateway ->
            val path = String.format(
                "%s/%s/assets/%s/encrypted",
                fileStorageApi.filesDir(),
                accountNumber,
                assetId
            )
            fileGateway.delete(path)
        }.onErrorResumeNext { Completable.complete() }

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
        else databaseApi.rxCompletable { db -> db.transactionDao().save(txs) }
    }

    fun listTxs(
        bitmarkId: String,
        loadAsset: Boolean = false,
        isPending: Boolean = false,
        loadBlock: Boolean = false,
        limit: Int = 100
    ): Maybe<List<TransactionData>> {
        val status =
            if (isPending) arrayOf(PENDING, CONFIRMED) else arrayOf(CONFIRMED)
        return databaseApi.rxMaybe { db ->
            db.transactionDao()
                .listByBitmarkIdStatusLimitDesc(bitmarkId, status, limit)
        }.flatMap { txs ->
            if (loadAsset && txs.isNotEmpty()) {
                val assetIds =
                    txs.distinctBy { tx -> tx.assetId }.map { tx -> tx.assetId }

                val assetStreams = mutableListOf<Maybe<AssetData>>()
                for (id in assetIds) {
                    val assetStream = getAssetById(id)
                    assetStreams.add(assetStream)
                }

                Maybe.merge(assetStreams).doOnNext { asset ->
                    txs.filter { tx -> tx.assetId == asset.id }
                        .forEach { tx -> tx.asset = asset }
                }.lastOrError().flatMapMaybe { Maybe.just(txs) }

            } else Maybe.just(txs)

        }.flatMap { txs ->
            if (loadBlock && txs.isNotEmpty()) {
                val blockNumbers = txs.distinctBy { tx -> tx.blockNumber }
                    .map { tx -> tx.blockNumber }

                val blockStreams = mutableListOf<Maybe<BlockData>>()
                for (blkNo in blockNumbers) {
                    val blockStream = getBlockByNumber(blkNo)
                    blockStreams.add(blockStream)
                }

                Maybe.merge(blockStreams).doOnNext { block ->
                    txs.filter { tx ->
                        tx.blockNumber == block.number
                    }.forEach { tx -> tx.block = block }
                }.lastOrError().flatMapMaybe { Maybe.just(txs) }

            } else Maybe.just(txs)
        }
    }

    fun deleteTxsByBitmarkId(bitmarkId: String) =
        databaseApi.rxCompletable { db ->
            db.transactionDao().deleteTxsByBitmarkId(bitmarkId)
        }

    fun deleteTxsByBitmarkIds(bitmarkIds: List<String>) =
        databaseApi.rxCompletable { db ->
            db.transactionDao().deleteTxsByBitmarkIds(bitmarkIds)
        }

    //endregion Txs

}