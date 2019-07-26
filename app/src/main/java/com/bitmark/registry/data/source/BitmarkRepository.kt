package com.bitmark.registry.data.source

import com.bitmark.apiservice.params.TransferParams
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.BitmarkData.Status.TO_BE_DELETED
import com.bitmark.registry.data.model.BitmarkData.Status.TO_BE_TRANSFERRED
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.source.local.*
import com.bitmark.registry.data.source.remote.BitmarkRemoteDataSource
import com.bitmark.registry.util.encryption.SessionData
import com.bitmark.registry.util.extension.append
import io.reactivex.Completable
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

    fun setAssetFileSavedListener(listener: AssetFileSavedListener?) {
        localDataSource.setAssetFileSavedListener(listener)
    }

    // sync bitmarks from server and save to local db
    fun syncBitmarks(
        owner: String? = null,
        at: Long = 0,
        to: String = "earlier",
        limit: Int = 100,
        pending: Boolean = false,
        issuer: String? = null,
        refAssetId: String? = null,
        loadAsset: Boolean = true
    ): Single<List<BitmarkData>> {
        return remoteDataSource.listBitmarks(
            owner = owner,
            at = at,
            to = to,
            limit = limit,
            pending = pending,
            issuer = issuer,
            refAssetId = refAssetId,
            loadAsset = loadAsset
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

    // try to clean up bitmarks in local db first
    // then fetch bitmarks from local db and then fetch from server if no bitmark is returned from db
    fun listBitmarks(
        owner: String,
        at: Long,
        limit: Int
    ): Single<List<BitmarkData>> {
        return cleanupBitmark(owner).andThen(
            localDataSource.listBitmarksByOwnerOffsetLimitDesc(
                owner,
                at,
                limit
            )
        )
            .map { bitmarks -> bitmarks.filter { b -> b.status != TO_BE_DELETED && b.status != TO_BE_TRANSFERRED } }
            .flatMap { bitmarks ->

                // if no local data returned, sync with remote data
                if (bitmarks.isEmpty()) syncBitmarks(
                    owner = owner,
                    at = at,
                    to = "earlier",
                    limit = limit
                )
                else {
                    Single.just(bitmarks)
                }
            }
    }

    // clean up bitmark is deleted from server side but not be reflected in local db
    // also update bitmarks to latest state if the previous delete/transfer action could not be sent to server
    fun cleanupBitmark(owner: String): Completable =
        localDataSource.listBitmarksByOwnerStatus(
            owner,
            listOf(TO_BE_DELETED, TO_BE_TRANSFERRED)
        ).flatMapCompletable { bitmarks ->
            if (bitmarks.isNullOrEmpty()) Completable.complete()
            else {
                val bitmarkIds = bitmarks.map { b -> b.id }

                remoteDataSource.listBitmarks(bitmarkIds = bitmarkIds)
                    .observeOn(Schedulers.io())
                    .flatMapCompletable { p ->

                        // the bitmarks're been updated in local but not be reflected in server
                        val usableBitmarks =
                            p.first.filter { b -> b.owner == owner }

                        // the bitmarks're been deleted or transferred in server but not be reflected to local
                        val unusableBitmarks =
                            p.first.filter { b -> b.owner != owner }

                        val updateUsableBitmarksStream =
                            if (usableBitmarks.isNullOrEmpty()) Completable.complete() else localDataSource.saveBitmarks(
                                usableBitmarks
                            )

                        val deleteBitmarksStream: Completable =
                            if (unusableBitmarks.isNullOrEmpty()) {
                                Completable.complete()
                            } else {
                                val deleteBitmarkStreams =
                                    mutableListOf<Completable>()
                                unusableBitmarks.forEach { b ->
                                    deleteBitmarkStreams.add(
                                        deleteStoredBitmark(
                                            owner,
                                            b.id,
                                            b.assetId
                                        )
                                    )
                                }

                                Completable.mergeDelayError(deleteBitmarkStreams)
                            }

                        Completable.mergeArray(
                            updateUsableBitmarksStream,
                            deleteBitmarksStream
                        )

                    }.onErrorResumeNext { Completable.complete() }
            }
        }

    // find out all pending bitmarks in local db
    fun listStoredPendingBitmarks(owner: String): Single<List<BitmarkData>> =
        Single.zip(
            localDataSource.listBitmarksByOwnerStatus(
                owner,
                BitmarkData.Status.TRANSFERRING
            ),
            localDataSource.listBitmarksByOwnerStatus(
                owner,
                BitmarkData.Status.ISSUING
            ),
            BiFunction<List<BitmarkData>, List<BitmarkData>, List<BitmarkData>> { transferring, issuing ->
                mutableListOf<BitmarkData>().append(transferring, issuing)
            })

    fun maxStoredBitmarkOffset(): Single<Long> =
        localDataSource.maxBitmarkOffset()

    fun markBitmarkSeen(bitmarkId: String): Single<String> =
        localDataSource.markBitmarkSeen(bitmarkId)

    fun countUsableBitmarks(owner: String): Single<Long> =
        localDataSource.countUsableBitmarks(owner)

    // try to update bitmark status to temporary status that mark is will be deleted
    // then transfer it to zero address then delete this bitmark in local db and related txs as well
    // try to clean up redundant asset file if no related bitmark is available
    fun deleteBitmark(
        params: TransferParams,
        bitmarkId: String,
        assetId: String
    ): Completable {
        return localDataSource.updateBitmarkStatus(
            bitmarkId,
            TO_BE_DELETED
        ).andThen(remoteDataSource.transfer(params))
            .flatMapCompletable {
                localDataSource.getBitmarkById(bitmarkId).map { b -> b.owner }
                    .flatMapCompletable { owner ->
                        deleteStoredBitmark(
                            owner,
                            bitmarkId,
                            assetId
                        )
                    }
            }
    }

    // delete bitmark in local db, also related asset file if needed, txs, asset
    private fun deleteStoredBitmark(
        owner: String,
        bitmarkId: String,
        assetId: String
    ) = Completable.mergeArrayDelayError(
        localDataSource.deleteBitmarkById(bitmarkId),
        localDataSource.deleteIrrelevantTxsByBitmarkId(owner, bitmarkId)
    ).andThen(localDataSource.checkRedundantAsset(assetId)
        .flatMapCompletable { redundant ->
            if (redundant) {
                // TODO consider whether need to delete file
                localDataSource.deleteAssetFile(
                    owner,
                    assetId
                )
            } else Completable.complete()
        })


    fun listStoredBitmarkRefSameAsset(assetId: String) =
        localDataSource.listBitmarkRefSameAsset(assetId)

    // change local bitmark status to TO_BE_TRANSFERRED
    // then send transfer request to server. Finally clean up corresponding bitmark in local db
    fun transferBitmark(
        params: TransferParams,
        owner: String,
        bitmarkId: String,
        assetId: String
    ): Completable = localDataSource.updateBitmarkStatus(
        bitmarkId,
        TO_BE_TRANSFERRED
    ).andThen(remoteDataSource.transfer(params)).flatMapCompletable {
        deleteStoredBitmark(owner, bitmarkId, assetId)
    }

    // sync txs with remote server and also save to local db
    fun syncTxs(
        owner: String? = null,
        sent: Boolean = false,
        bitmarkId: String? = null,
        loadAsset: Boolean = true,
        isPending: Boolean = false,
        loadBlock: Boolean = true,
        at: Long = 0,
        to: String = "earlier",
        limit: Int = 100
    ): Single<List<TransactionData>> =
        remoteDataSource.listTxs(
            owner = owner,
            sent = sent,
            bitmarkId = bitmarkId,
            loadAsset = loadAsset,
            isPending = isPending,
            loadBlock = loadBlock,
            at = at,
            to = to,
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

    // fetch txs by bitmark id in local db
    fun listTxs(
        bitmarkId: String,
        loadAsset: Boolean = false,
        isPending: Boolean = false,
        loadBlock: Boolean = false,
        limit: Int = 100
    ): Single<List<TransactionData>> {
        return localDataSource.listTxs(
            bitmarkId,
            loadAsset,
            isPending,
            loadBlock,
            limit
        )
    }

    fun listRelevantTxs(
        owner: String,
        offset: Long,
        limit: Int = 100
    ): Single<List<TransactionData>> {
        return localDataSource.listRelevantTxs(
            owner = owner,
            offset = offset,
            loadAsset = true,
            loadBlock = true,
            limit = limit
        ).flatMap { txs ->
            if (txs.isNotEmpty()) Single.just(txs)
            else {
                // sync remote data and save to local db
                syncTxs(
                    owner = owner,
                    sent = true,
                    loadAsset = true,
                    loadBlock = true,
                    at = offset,
                    to = "earlier",
                    limit = limit
                )
            }
        }
    }

    fun maxStoredRelevantTxOffset(owner: String): Single<Long> =
        localDataSource.maxRelevantTxOffset(owner)

    fun listStoredPendingTxs(owner: String): Single<List<TransactionData>> =
        localDataSource.listTxsByOwnerStatus(
            owner,
            TransactionData.Status.PENDING
        )

    fun checkAssetFile(
        owner: String,
        assetId: String
    ): Single<Pair<String, File?>> =
        localDataSource.checkAssetFile(owner, assetId)


    fun downloadAssetFile(assetId: String, sender: String, receiver: String) =
        remoteDataSource.downloadAssetFile(assetId, sender, receiver)

    fun saveAssetFile(
        owner: String,
        assetId: String,
        fileName: String,
        content: ByteArray
    ) = localDataSource.saveAssetFile(owner, assetId, fileName, content)

    fun deleteRemoteAssetFile(
        assetId: String,
        sender: String,
        receiver: String
    ) =
        remoteDataSource.deleteAssetFile(assetId, sender, receiver)

    fun checkExistingRemoteAssetFile(
        assetId: String,
        sender: String
    ) =
        remoteDataSource.checkExistingAssetFile(assetId, sender)

    fun grantAccessAssetFile(assetId: String, sender: String, access: String) =
        remoteDataSource.grantAccessAssetFile(assetId, sender, access)

    // save byte array need to be uploaded to local storage then upload to server
    // after that try to clear the encrypted file
    fun uploadAssetFile(
        assetId: String,
        owner: String,
        sessionData: SessionData,
        access: String,
        fileName: String,
        fileBytes: ByteArray
    ): Completable = localDataSource.saveEncryptedAssetFile(
        owner,
        assetId,
        fileName,
        fileBytes
    ).flatMapCompletable { file ->
        remoteDataSource.uploadAssetFile(
            assetId,
            owner,
            sessionData,
            access,
            file
        )
    }.andThen(localDataSource.deleteEncryptedAssetFile(owner, assetId))

}