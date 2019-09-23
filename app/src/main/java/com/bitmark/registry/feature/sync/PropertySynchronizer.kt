package com.bitmark.registry.feature.sync

import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.Constant
import com.bitmark.registry.logging.Tracer
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable


/**
 * @author Hieu Pham
 * @since 2019-08-16
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertySynchronizer(
    private val bitmarkRepo: BitmarkRepository,
    private val accountRepo: AccountRepository
) {

    companion object {
        private const val TAG = "PropertySynchronizer"

        private const val ITEM_PER_PAGE = 100
    }

    private lateinit var compositeDisposable: CompositeDisposable

    private var minBitmarkOffset = -1L

    private var minTxsOffset = -1L

    fun start() {
        Tracer.DEBUG.log(TAG, "Starting...")
        compositeDisposable = CompositeDisposable()
        syncBitmarks()
        syncTxs()
        syncClaimRequests()
    }

    fun stop() {
        compositeDisposable.dispose()
        Tracer.DEBUG.log(TAG, "Stopped")
    }

    private fun syncBitmarks() {

        compositeDisposable.add(accountRepo.getAccountNumber().flatMap { accountNumber ->
            val offsetStream =
                if (minBitmarkOffset == -1L) {
                    bitmarkRepo.minStoredBitmarkOffset()
                } else {
                    Single.just(minBitmarkOffset)
                }

            offsetStream.flatMap { offset ->
                bitmarkRepo.syncBitmarks(
                    owner = accountNumber,
                    at = offset,
                    to = "earlier",
                    pending = true,
                    loadAsset = true,
                    limit = ITEM_PER_PAGE
                ).retry(1)
            }.flatMap { bitmarks ->
                if (bitmarks.isEmpty()) {
                    Single.just(bitmarks)
                } else {
                    val assets = bitmarks.filter { b -> b.asset != null }
                        .map { b -> b.asset }.distinctBy { a -> a!!.id }
                    val streams = assets.map { asset ->
                        bitmarkRepo.checkAssetFile(
                            accountNumber,
                            asset!!.id
                        ).flatMapCompletable { p ->
                            val file = p.second
                            if (file == null) {
                                Completable.complete()
                            } else {
                                val type = AssetData.determineAssetType(
                                    asset.metadata,
                                    file
                                )
                                bitmarkRepo.updateAssetType(asset.id, type)
                            }
                        }
                    }
                    Completable.mergeDelayError(streams)
                        .andThen(Single.just(bitmarks))
                }
            }

        }.subscribe { bitmarks, e ->
            if (e == null) {
                minBitmarkOffset =
                    bitmarks.minBy { b -> b.offset }?.offset ?: return@subscribe
                Tracer.DEBUG.log(TAG, "minBitmarkOffset: $minBitmarkOffset")
                if (bitmarks.size == ITEM_PER_PAGE) {
                    Tracer.DEBUG.log(TAG, "resync bitmarks recursively")
                    syncBitmarks()
                } else {
                    Tracer.DEBUG.log(
                        TAG,
                        "stop sync bitmarks since fetched all"
                    )
                }
            }
        })
    }

    private fun syncTxs() {

        compositeDisposable.add(accountRepo.getAccountNumber().flatMap { accountNumber ->
            val offsetStream =
                if (minTxsOffset == -1L) {
                    bitmarkRepo.minStoredRelevantTxOffset(accountNumber)
                } else {
                    Single.just(minTxsOffset)
                }

            offsetStream.flatMap { offset ->
                bitmarkRepo.syncTxs(
                    owner = accountNumber,
                    sent = true,
                    at = offset,
                    to = "earlier",
                    isPending = true,
                    loadAsset = true,
                    loadBlock = true,
                    limit = ITEM_PER_PAGE
                )
            }.retry(1)
        }.subscribe { txs, e ->
            if (e == null) {
                minTxsOffset =
                    txs.minBy { t -> t.offset }?.offset ?: return@subscribe
                Tracer.DEBUG.log(TAG, "minTxsOffset: $minTxsOffset")
                if (txs.size == ITEM_PER_PAGE) {
                    Tracer.DEBUG.log(TAG, "resync txs recursively")
                    syncTxs()
                } else {
                    Tracer.DEBUG.log(TAG, "stop sync txs since fetched all")
                }
            }
        })
    }

    private fun syncClaimRequests() {
        compositeDisposable.add(
            bitmarkRepo.syncAssetClaimingRequest(Constant.OMNISCIENT_ASSET_ID).retry(
                1
            ).subscribe { _, _ ->
                Tracer.DEBUG.log(TAG, "sync claim request done")
            })
    }

}