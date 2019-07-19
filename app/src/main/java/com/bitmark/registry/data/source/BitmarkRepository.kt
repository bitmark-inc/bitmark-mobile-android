package com.bitmark.registry.data.source

import com.bitmark.apiservice.params.TransferParams
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.BitmarkData.Status.TO_BE_DELETED
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.source.local.BitmarkDeletedListener
import com.bitmark.registry.data.source.local.BitmarkInsertedListener
import com.bitmark.registry.data.source.local.BitmarkLocalDataSource
import com.bitmark.registry.data.source.local.BitmarkStatusChangedListener
import com.bitmark.registry.data.source.remote.BitmarkRemoteDataSource
import com.bitmark.registry.util.extension.append
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.io.File


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BitmarkRepository(
    private val localDataSource: BitmarkLocalDataSource,
    private val remoteDataSource: BitmarkRemoteDataSource
) : AbsRepository() {

    fun setBitmarkDeletedListener(listener: BitmarkDeletedListener?) {
        localDataSource.setBitmarkDeletedListener(listener)
    }

    fun setBitmarkStatusChangedListener(listener: BitmarkStatusChangedListener?) {
        localDataSource.setBitmarkStatusChangedListener(listener)
    }

    fun setBitmarkInsertedListener(listener: BitmarkInsertedListener?) {
        localDataSource.setBitmarkInsertedListener(listener)
    }

    fun syncBitmarks(
        owner: String? = null,
        at: Long = 0,
        to: String = "earlier",
        limit: Int = 100,
        pending: Boolean = false,
        issuer: String? = null,
        refAssetId: String? = null
    ): Single<List<BitmarkData>> {
        return remoteDataSource.listBitmarks(
            owner = owner,
            at = at,
            to = to,
            limit = limit,
            pending = pending,
            issuer = issuer,
            refAssetId = refAssetId,
            loadAsset = true
        ).observeOn(Schedulers.computation()).flatMap { p ->
            localDataSource.saveAssets(p.second)
                .andThen(localDataSource.saveBitmarks(p.first))
                .andThen(Single.just(p))
        }.map { p ->
            val bitmarks = p.first
            val assets = p.second
            assets.forEach { asset ->
                bitmarks.filter { b -> b.assetId == asset.id }
                    .forEach { b -> b.asset = asset }
            }
            bitmarks
        }
    }

    fun listBitmarks(
        owner: String,
        at: Long,
        limit: Int
    ): Maybe<List<BitmarkData>> {
        return localDataSource.countBitmarks().flatMapMaybe { count ->
            if (count == 0L) {
                // database is empty, sync with remote data
                syncBitmarks(owner = owner, limit = limit).toMaybe()
            } else {
                cleanupBitmark(owner).andThen(
                    localDataSource.listBitmarksByOffsetLimitDesc(
                        at,
                        limit
                    )
                )
                    .map { bitmarks -> bitmarks.filter { b -> b.status != TO_BE_DELETED } }
                    .flatMap { bitmarks ->

                        // if database return empty, sync with remote data
                        if (bitmarks.isEmpty()) syncBitmarks(
                            owner = owner,
                            at = at,
                            to = "earlier",
                            limit = limit
                        ).toMaybe()
                        else Maybe.just(bitmarks)
                    }
                    .flatMap(attachAssetFunc())
            }
        }
    }

    private fun cleanupBitmark(owner: String): Completable =
        localDataSource.listBitmarksByStatus(TO_BE_DELETED).flatMapCompletable { bitmarks ->
            if (bitmarks.isNullOrEmpty()) Completable.complete()
            else {
                val bitmarkIds = bitmarks.map { b -> b.id }

                remoteDataSource.listBitmarks(bitmarkIds = bitmarkIds)
                    .flatMapCompletable { p ->

                        // the bitmarks're been updated in local to be deleted but not be reflected in server
                        val usableBitmarks =
                            p.first.filter { b -> b.owner == owner }

                        // the bitmarks're been deleted in server but not be reflected to local
                        val deletedBitmarkIds =
                            p.first.filter { b -> b.owner != owner }
                                .map { b -> b.id }

                        val updateUsableBitmarksStream =
                            if (usableBitmarks.isNullOrEmpty()) Completable.complete() else localDataSource.saveBitmarks(
                                usableBitmarks
                            )

                        val deleteBitmarksStream =
                            if (deletedBitmarkIds.isNullOrEmpty()) Completable.complete() else Completable.mergeArray(
                                localDataSource.deleteTxsByBitmarkIds(
                                    deletedBitmarkIds
                                ),
                                localDataSource.deleteBitmarkByIds(
                                    deletedBitmarkIds
                                )
                            )

                        Completable.mergeArray(
                            updateUsableBitmarksStream,
                            deleteBitmarksStream
                        )

                    }.onErrorResumeNext { Completable.complete() }
            }
        }

    fun listStoredPendingBitmarks(): Maybe<List<BitmarkData>> = Maybe.zip(
        localDataSource.listBitmarksByStatus(BitmarkData.Status.TRANSFERRING),
        localDataSource.listBitmarksByStatus(BitmarkData.Status.ISSUING),
        BiFunction<List<BitmarkData>, List<BitmarkData>, List<BitmarkData>> { transferring, issuing ->
            mutableListOf<BitmarkData>().append(transferring, issuing)
        }).flatMap(attachAssetFunc())

    private fun attachAssetFunc(): (List<BitmarkData>) -> Maybe<List<BitmarkData>> =
        { bitmarks ->

            if (bitmarks.isEmpty()) {
                Maybe.just(bitmarks)
            } else {
                // map asset to bitmark
                val assetStreams = mutableListOf<Maybe<AssetData>>()
                for (bitmark in bitmarks) {
                    assetStreams.add(
                        localDataSource.getAssetById(
                            bitmark.assetId
                        )
                    )
                }
                Maybe.merge(assetStreams).doOnNext { asset ->
                    bitmarks.filter { b -> b.assetId == asset.id }
                        .forEach { b -> b.asset = asset }
                }.lastOrError()
                    .flatMapMaybe { Maybe.just(bitmarks) }
            }
        }

    fun maxStoredBitmarkOffset(): Single<Long> =
        localDataSource.maxBitmarkOffset()

    fun minStoredBitmarkOffset(): Single<Long> =
        localDataSource.minBitmarkOffset()

    fun markBitmarkSeen(bitmarkId: String): Single<String> =
        localDataSource.markBitmarkSeen(bitmarkId)

    fun checkAssetFile(
        accountNumber: String,
        assetId: String
    ): Single<Pair<String, File?>> =
        localDataSource.checkAssetFile(accountNumber, assetId)

    fun countUsableBitmarks(): Single<Long> =
        localDataSource.countUsableBitmarks()

    fun syncTxs(
        bitmarkId: String,
        loadAsset: Boolean = false,
        isPending: Boolean = false,
        loadBlock: Boolean = false,
        limit: Int = 100
    ): Single<List<TransactionData>> =
        remoteDataSource.listTxs(
            bitmarkId = bitmarkId,
            loadAsset = loadAsset,
            isPending = isPending,
            loadBlock = loadBlock,
            limit = limit
        ).observeOn(Schedulers.computation()).flatMap { t ->
            localDataSource.saveBlocks(t.third)
                .andThen(localDataSource.saveAssets(t.second))
                .andThen(localDataSource.saveTxs(t.first))
                .andThen(Single.just(t))
        }.map { t ->
            val txs = t.first
            val assets = t.second
            val blocks = t.third

            assets.forEach { asset ->
                txs.filter { tx -> tx.assetId == asset.id }
                    .forEach { tx -> tx.asset = asset }
            }

            blocks.forEach { block ->
                txs.filter { tx -> tx.blockNumber == block.number }
                    .forEach { tx -> tx.block = block }
            }

            txs
        }

    fun listTxs(
        bitmarkId: String,
        loadAsset: Boolean = false,
        isPending: Boolean = false,
        loadBlock: Boolean = false,
        limit: Int = 100
    ): Maybe<List<TransactionData>> {
        return localDataSource.listTxs(
            bitmarkId,
            loadAsset,
            isPending,
            loadBlock,
            limit
        )
    }

    fun deleteBitmark(
        params: TransferParams,
        bitmarkId: String,
        assetId: String,
        accountNumber: String
    ): Completable {
        val deletedRedundantAssetFile =
            localDataSource.checkRedundantAsset(assetId)
                .flatMapCompletable { redundant ->
                    if (redundant) {
                        localDataSource.deleteAssetFile(accountNumber, assetId)
                    } else Completable.complete()
                }
        return localDataSource.updateBitmarkStatus(
            bitmarkId,
            TO_BE_DELETED
        ).andThen(remoteDataSource.transfer(params)).flatMapCompletable {
            Completable.mergeArray(
                localDataSource.deleteBitmarkById(bitmarkId),
                localDataSource.deleteTxsByBitmarkId(bitmarkId)
            )
        }.andThen(deletedRedundantAssetFile)
    }


    fun downloadAssetFile(assetId: String, sender: String, receiver: String) =
        remoteDataSource.downloadAssetFile(assetId, sender, receiver)

    fun saveAssetFile(
        accountNumber: String,
        assetId: String,
        fileName: String,
        content: ByteArray
    ) = localDataSource.saveAssetFile(accountNumber, assetId, fileName, content)

    fun deleteServerAssetFile(
        assetId: String,
        sender: String,
        receiver: String
    ) =
        remoteDataSource.deleteAssetFile(assetId, sender, receiver)

}