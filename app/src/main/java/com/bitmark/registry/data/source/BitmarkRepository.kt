/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source

import com.bitmark.apiservice.params.IssuanceParams
import com.bitmark.apiservice.params.RegistrationParams
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.registry.data.ext.isDbRecNotFoundError
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.BitmarkData.Status.*
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.model.entity.AssetDataL
import com.bitmark.registry.data.source.local.BitmarkLocalDataSource
import com.bitmark.registry.data.source.local.event.*
import com.bitmark.registry.data.source.remote.BitmarkRemoteDataSource
import com.bitmark.registry.util.encryption.SessionData
import com.bitmark.registry.util.extension.append
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.io.File

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

    fun setBitmarkSavedListener(listener: BitmarkSavedListener?) {
        localDataSource.setBitmarkSavedListener(listener)
    }

    fun setAssetFileSavedListener(listener: AssetFileSavedListener?) {
        localDataSource.setAssetFileSavedListener(listener)
    }

    fun setTxsSavedListener(listener: TxSavedListener?) {
        localDataSource.setTxsSavedListener(listener)
    }

    fun setBitmarkSeenListener(listener: BitmarkSeenListener?) {
        localDataSource.setBitmarkSeenListener(listener)
    }

    fun setAssetSavedListener(listener: AssetSavedListener?) {
        localDataSource.setAssetSavedListener(listener)
    }

    fun setAssetTypeChangedListener(listener: AssetTypeChangedListener?) {
        localDataSource.setAssetTypeChangedListener(listener)
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
        loadAsset: Boolean = true,
        bitmarkIds: List<String>? = null
    ): Single<List<BitmarkData>> {
        return remoteDataSource.listBitmarks(
            owner = owner,
            at = at,
            to = to,
            limit = limit,
            pending = pending,
            issuer = issuer,
            refAssetId = refAssetId,
            loadAsset = loadAsset,
            bitmarkIds = bitmarkIds
        ).observeOn(Schedulers.io()).flatMap { p ->
            localDataSource.saveAssets(p.second).flatMap { asset ->
                localDataSource.saveBitmarks(p.first)
                    .map { bitmark -> Pair(bitmark, asset) }
            }
        }.map { p ->
            val bitmarks = p.first
            val assets = p.second
            assets.forEach { asset ->
                bitmarks.filter { b -> b.assetId == asset.id }
                    .forEach { b -> b.setAsset(asset) }
            }
            bitmarks
        }
    }

    // sync pending bitmarks in local db with server
    fun syncPendingBitmarks(owner: String) =
        localDataSource.listBitmarksByOwnerStatus(
            owner,
            listOf(ISSUING, TRANSFERRING)
        ).flatMapCompletable { bitmarks ->
            if (bitmarks.isEmpty()) Completable.complete()
            else {
                val bitmarkIds = bitmarks.map { b -> b.id }
                val streams = mutableListOf<Completable>()
                bitmarkIds.forEach { id ->
                    streams.add(
                        syncBitmark(
                            id,
                            false
                        ).ignoreElement()
                    )
                }
                Completable.mergeDelayError(streams)
            }
        }

    fun listBitmarks(
        owner: String,
        at: Long,
        limit: Int
    ): Single<List<BitmarkData>> {
        return localDataSource.listBitmarksByOwnerOffsetLimitDesc(
            owner,
            at,
            limit
        )
            .map { bitmarks -> bitmarks.filter { b -> b.status != TO_BE_DELETED && b.status != TO_BE_TRANSFERRED } }
            .flatMap { bitmarks ->

                // if no local data returned, sync with remote data
                if (bitmarks.isEmpty()) {
                    syncBitmarks(
                        owner = owner,
                        at = at,
                        to = "earlier",
                        limit = limit
                    )
                } else {
                    Single.just(bitmarks)
                }
            }
    }

    fun listStoredOwnedBitmarks(owner: String) =
        localDataSource.listBitmarksByOwnerStatus(
            owner,
            listOf(SETTLED, ISSUING)
        )

    // sync bitmark with remote server then save to local db
    fun syncBitmark(bitmarkId: String, loadAsset: Boolean = false) =
        remoteDataSource.getBitmark(bitmarkId, loadAsset).flatMap { p ->
            val bitmarkR = p.first
            val assetR = p.second
            val saveBitmarkStream =
                if (bitmarkR == null) {
                    Single.error<BitmarkData>(Throwable("Resource not found"))
                } else {
                    localDataSource.saveBitmark(bitmarkR)
                }
            val saveAssetStream =
                if (assetR == null) {
                    Completable.complete()
                } else {
                    localDataSource.saveAsset(assetR).ignoreElement()
                }
            saveAssetStream.andThen(saveBitmarkStream)
        }

    // clean up bitmark is deleted from server side but not be reflected in local db
    // also update bitmarks to latest state if the previous delete/transfer action could not be sent to server
    fun syncUpBitmark(owner: String): Completable =
        localDataSource.listBitmarksByOwnerStatus(
            owner,
            listOf(TO_BE_DELETED, TO_BE_TRANSFERRED)
        ).flatMapCompletable { bitmarks ->
            if (bitmarks.isNullOrEmpty()) {
                Completable.complete()
            } else {
                val bitmarkIds = bitmarks.map { b -> b.id }

                val streams = mutableListOf<Completable>()

                bitmarkIds.forEach { id ->
                    val stream = remoteDataSource.getBitmark(id, false)
                        .observeOn(Schedulers.io()).flatMapCompletable { p ->
                            val bitmark = p.first!!
                            val usable = bitmark.owner == owner
                            if (usable) {
                                localDataSource.saveBitmark(bitmark)
                                    .ignoreElement()
                            } else {
                                deleteStoredBitmark(
                                    owner,
                                    bitmark.id,
                                    bitmark.assetId
                                )
                            }
                        }
                    streams.add(stream)
                }
                Completable.mergeDelayError(streams)
            }
        }

    fun cleanupBitmark(owner: String) =
        Completable.mergeArrayDelayError(
            localDataSource.deleteNotOwnBitmarks(owner),
            fetchNewTransferredTxs(owner).map { txs ->
                txs.map { tx ->
                    Pair(
                        tx.bitmarkId,
                        tx.assetId
                    )
                }
            }.flatMapCompletable { pairs ->
                val streams = pairs.map { p ->
                    deleteStoredBitmark(
                        owner,
                        p.first,
                        p.second
                    )
                }
                Completable.mergeDelayError(streams)
            })

    private fun fetchNewTransferredTxs(owner: String) =
        maxStoredRelevantTxOffset(owner).flatMap { offset ->
            remoteDataSource.listTxs(
                owner = owner,
                sent = true,
                loadAsset = false,
                loadBlock = false,
                isPending = true,
                at = offset,
                to = "later"
            )
                .map { t ->
                    t.first.filter { tx -> tx.previousOwner == owner }
                }
        }

    // find out all pending bitmarks in local db
    fun listStoredPendingBitmarks(owner: String): Single<List<BitmarkData>> =
        Single.zip(
            localDataSource.listBitmarksByOwnerStatus(
                owner,
                TRANSFERRING
            ),
            localDataSource.listBitmarksByOwnerStatus(
                owner,
                ISSUING
            ),
            BiFunction<List<BitmarkData>, List<BitmarkData>, List<BitmarkData>> { transferring, issuing ->
                mutableListOf<BitmarkData>().append(transferring, issuing)
            })

    fun maxStoredBitmarkOffset(): Single<Long> =
        localDataSource.maxBitmarkOffset()

    fun minStoredPendingBitmarkOffset() =
        localDataSource.minBitmarkOffset(arrayOf(ISSUING))

    fun syncLatestBitmarks(owner: String) =
        maxStoredBitmarkOffset().flatMap { offset ->
            syncBitmarks(
                owner = owner,
                at = offset,
                to = "later",
                pending = true,
                loadAsset = true
            )
        }

    fun minStoredBitmarkOffset(): Single<Long> =
        localDataSource.minBitmarkOffset()

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
            }.onErrorResumeNext { e ->
                if (e.isDbRecNotFoundError()) Completable.complete() else Completable.error(
                    e
                )
            }
    }

    // delete bitmark in local db, also related asset file if needed, txs, asset
    fun deleteStoredBitmark(
        owner: String,
        bitmarkId: String,
        assetId: String
    ) = Completable.mergeArrayDelayError(
        localDataSource.deleteBitmarkById(bitmarkId),
        localDataSource.deleteIrrelevantTxsByBitmarkId(owner, bitmarkId)
    ).andThen(localDataSource.checkRedundantAsset(assetId)
        .flatMapCompletable { redundant ->
            if (redundant) {
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

    fun issueBitmark(params: IssuanceParams) =
        remoteDataSource.issueBitmark(params).flatMapCompletable { bitmarkIds ->

            val syncTxsStream = mutableListOf<Completable>()
            bitmarkIds.forEach { id ->
                syncTxsStream.add(
                    syncTxs(
                        sent = true,
                        loadBlock = true,
                        loadAsset = true,
                        isPending = true,
                        bitmarkId = id
                    ).ignoreElement()
                )
            }

            Completable.mergeArrayDelayError(
                syncBitmarks(
                    bitmarkIds = bitmarkIds,
                    pending = true,
                    loadAsset = true
                ).ignoreElement(), Completable.merge(syncTxsStream)
            )
        }

    fun getStoredBitmarkById(id: String) = localDataSource.getBitmarkById(id)

    fun checkUnseenBitmark(owner: String) =
        localDataSource.checkUnseenBitmark(owner)

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

            localDataSource.saveAssets(t.second).flatMap { asset ->
                localDataSource.saveBlocks(t.third).andThen(Single.just(asset))
            }.flatMap { asset ->
                localDataSource.saveTxs(t.first)
                    .andThen(Single.just(Triple(t.first, asset, t.third)))
            }
        }.map { t ->
            val txs = t.first.map { tx -> TransactionData(tx) }
            val assets = t.second
            val blocks = t.third

            assets.forEach { asset ->
                txs.filter { tx -> tx.assetId == asset.id }
                    .forEach { tx -> tx.assetData = listOf(asset) }
            }

            blocks.forEach { block ->
                txs.filter { tx -> tx.blockNumber == block.number }
                    .forEach { tx -> tx.blockData = listOf(block) }
            }

            txs
        }

    // sync all pending txs in local database with server
    fun syncPendingTxs(stakeholder: String) =
        localDataSource.listRelevantTxsByStatus(
            stakeholder,
            TransactionData.Status.PENDING
        ).flatMapCompletable { txs ->
            if (txs.isEmpty()) Completable.complete()
            else {
                val bitmarkIds =
                    txs.distinctBy { t -> t.bitmarkId }.map { b -> b.id }
                val syncTxsStream =
                    mutableListOf<Single<List<TransactionData>>>()
                bitmarkIds.forEach { id ->
                    syncTxsStream.add(
                        syncTxs(
                            bitmarkId = id,
                            loadAsset = false,
                            loadBlock = true,
                            sent = true
                        )
                    )
                }
                Single.merge(syncTxsStream).ignoreElements()
            }
        }

    // fetch txs by bitmark id in local db
    fun listTxs(
        bitmarkId: String,
        isPending: Boolean = false,
        limit: Int = 100
    ): Single<List<TransactionData>> {
        return localDataSource.listTxs(
            bitmarkId,
            isPending,
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

    fun minStoredRelevantPendingTxs(owner: String) =
        localDataSource.minRelevantTxOffset(
            owner,
            arrayOf(TransactionData.Status.PENDING)
        )

    fun syncLatestRelevantTxs(owner: String) =
        maxStoredRelevantTxOffset(owner).flatMap { offset ->
            syncTxs(
                owner = owner,
                at = offset,
                to = "later",
                sent = true,
                isPending = true
            )
        }

    fun minStoredRelevantTxOffset(owner: String): Single<Long> =
        localDataSource.minRelevantTxOffset(owner)

    fun listStoredRelevantPendingTxs(owner: String): Single<List<TransactionData>> =
        localDataSource.listRelevantTxsByStatus(
            owner,
            TransactionData.Status.PENDING
        )

    fun getStoredTxById(id: String) = localDataSource.getTxById(id)

    fun checkAssetFile(
        owner: String,
        assetId: String
    ): Single<Pair<String, File?>> =
        localDataSource.checkAssetFile(owner, assetId)


    fun downloadAssetFile(
        assetId: String,
        sender: String,
        receiver: String,
        progress: (Int) -> Unit
    ) =
        remoteDataSource.downloadAssetFile(assetId, sender, receiver, progress)

    fun saveAssetFile(
        owner: String,
        assetId: String,
        fileName: String,
        content: ByteArray
    ) = localDataSource.saveAssetFile(owner, assetId, fileName, content)

    fun updateAssetType(assetId: String, newType: AssetData.Type) =
        localDataSource.updateAssetType(assetId, newType)

    fun listStoredAssetFile(accountNumber: String) =
        localDataSource.listStoredAssetFile(accountNumber)

    fun deleteRemoteAssetFile(
        assetId: String,
        sender: String,
        receiver: String
    ) =
        remoteDataSource.deleteAssetFile(assetId, sender, receiver)

    fun deleteStoredAssetFiles(accountNumber: String) =
        localDataSource.deleteAssetFiles(accountNumber)

    fun checkExistingRemoteAssetFile(
        assetId: String,
        sender: String
    ) =
        remoteDataSource.checkExistingAssetFile(assetId, sender)

    fun grantAccessAssetFile(assetId: String, sender: String, access: String) =
        remoteDataSource.grantAccessAssetFile(assetId, sender, access)

    fun getDownloadableAssets(receiver: String) =
        remoteDataSource.getDownloadableAssets(receiver)

    // save byte array need to be uploaded to local storage then upload to server
    // after that try to clear the encrypted file
    fun uploadAssetFile(
        assetId: String,
        owner: String,
        sessionData: SessionData,
        access: String,
        fileName: String,
        fileBytes: ByteArray,
        progress: (Int) -> Unit
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
            file,
            progress
        )
    }.andThen(localDataSource.deleteEncryptedAssetFile(owner, assetId))

    fun getAsset(id: String) =
        localDataSource.getAssetById(id).onErrorResumeNext { e ->
            if (e.isDbRecNotFoundError()) {
                remoteDataSource.getAsset(id).flatMap { assetR ->
                    val assetType =
                        AssetData.determineAssetType(metadata = assetR.metadata)
                    localDataSource.saveAsset(
                        assetR,
                        AssetDataL(
                            assetId = assetR.id,
                            type = assetType
                        )
                    )
                }
            } else {
                Single.error(e)
            }
        }

    fun registerAsset(params: RegistrationParams) =
        remoteDataSource.registerAsset(params).flatMap { assetId ->
            remoteDataSource.getAsset(assetId).flatMap { assetR ->
                val assetType =
                    AssetData.determineAssetType(metadata = assetR.metadata)
                localDataSource.saveAsset(
                    assetR,
                    AssetDataL(
                        assetId = assetR.id,
                        type = assetType
                    )
                )
            }.map { asset -> asset.id }
        }

    fun getAssetClaimingInfo(assetId: String) =
        remoteDataSource.getAssetClaimingInfo(assetId)

    fun syncAssetClaimingRequest(assetId: String) =
        remoteDataSource.getAssetClaimRequests(
            assetId
        ).map { res -> res.outgoingRequests }
            .flatMap { outgoingRequests ->
                if (outgoingRequests.isEmpty()) Single.just(
                    outgoingRequests
                ) else localDataSource.saveAssetClaimings(
                    outgoingRequests
                ).andThen(
                    Single.just(
                        outgoingRequests
                    )
                )
            }

    fun listAssetClaimingRequest(assetId: String, from: String, to: String) =
        localDataSource.listAssetClaimingRequests(
            assetId,
            from,
            to
        ).flatMap { claimRequests ->
            if (claimRequests.isEmpty()) {
                syncAssetClaimingRequest(assetId).flatMap {
                    localDataSource.listAssetClaimingRequests(
                        assetId,
                        from,
                        to
                    )
                }
            } else {
                Single.just(claimRequests)
            }
        }.flatMap { claimRequests ->
            if (claimRequests.isEmpty()) Single.just(claimRequests)
            else {
                getAsset(assetId).flatMap { asset ->
                    claimRequests.forEach { c -> c.asset = asset }
                    Single.just(claimRequests)
                }
            }

        }

}