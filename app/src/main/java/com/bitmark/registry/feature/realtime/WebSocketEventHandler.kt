package com.bitmark.registry.feature.realtime

import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.util.RxCompletableChunkExecutor
import com.bitmark.registry.data.ext.isDbRecNotFoundError
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-08-23
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class WebSocketEventHandler @Inject constructor(
    private val wsEventBus: WebSocketEventBus,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository
) {

    companion object {
        private const val TAG = "WebSocketEventHandler"

        private const val CONCURRENT_TASKS_COUNT = 20
    }

    private val executor =
        RxCompletableChunkExecutor(
            CONCURRENT_TASKS_COUNT,
            TAG
        )

    fun start() {
        wsEventBus.bitmarkChangedPublisher.subscribe(this) { m ->
            val bitmarkId = m["bitmark_id"] as String
            val presence = m["presence"] as Boolean
            executor.execute(processBitmarkChangedEvent(bitmarkId, presence))
        }

        wsEventBus.newPendingTxPublisher.subscribe(this) { m ->
            val owner = m["owner"] ?: return@subscribe
            val previousTxId = m["prev_tx_id"] ?: return@subscribe
            executor.execute(processNewPendingTxEvent(owner, previousTxId))
        }

        wsEventBus.newPendingIssuancePublisher.subscribe(this) { bitmarkId ->
            executor.execute(processNewPendingIssuance(bitmarkId))
        }
    }

    fun stop() {
        executor.shutdown()
    }

    private fun processBitmarkChangedEvent(
        bitmarkId: String,
        presence: Boolean
    ): Completable {

        val syncTxsStream =
            accountRepo.getAccountNumber().flatMap { accountNumber ->
                bitmarkRepo.syncTxs(
                    owner = accountNumber,
                    sent = true,
                    bitmarkId = bitmarkId,
                    loadAsset = true,
                    loadBlock = true
                )
            }.ignoreElement()

        val syncBitmarkStream = if (presence) bitmarkRepo.syncBitmark(
            bitmarkId,
            true
        ).ignoreElement() else Completable.complete()

        return Completable.mergeArrayDelayError(
            syncTxsStream,
            syncBitmarkStream
        )
    }

    private fun processNewPendingTxEvent(
        owner: String,
        prevTxId: String
    ): Completable {

        val deleteBmStream =
            fun(accountNumber: String) =
                bitmarkRepo.getStoredTxById(prevTxId).flatMapCompletable { tx ->
                    bitmarkRepo.deleteStoredBitmark(
                        accountNumber,
                        tx.bitmarkId,
                        tx.assetId
                    )
                }


        return accountRepo.getAccountNumber()
            .flatMapCompletable { accountNumber ->
                if (BuildConfig.ZERO_ADDRESS == owner) {
                    // outgoing tx for delete
                    deleteBmStream(accountNumber)
                } else {
                    bitmarkRepo.maxStoredRelevantTxOffset(accountNumber)
                        .flatMap { offset ->
                            bitmarkRepo.syncTxs(
                                owner = accountNumber,
                                sent = true,
                                isPending = true,
                                loadAsset = true,
                                loadBlock = true,
                                at = offset,
                                to = "later"
                            )
                        }.flatMapCompletable {
                            if (owner != accountNumber) {
                                // outgoing tx
                                deleteBmStream(accountNumber)
                            } else {
                                // incoming tx
                                bitmarkRepo.maxStoredBitmarkOffset()
                                    .flatMap { offset ->
                                        bitmarkRepo.syncBitmarks(
                                            owner = accountNumber,
                                            at = offset,
                                            to = "later",
                                            pending = true,
                                            loadAsset = true
                                        )
                                    }.ignoreElement()
                            }
                        }
                }
            }
    }

    private fun processNewPendingIssuance(bitmarkId: String) =
        bitmarkRepo.getStoredBitmarkById(bitmarkId).onErrorResumeNext { e ->
            if (e.isDbRecNotFoundError()) Single.zip(bitmarkRepo.syncBitmark(
                bitmarkId,
                true
            ), bitmarkRepo.syncTxs(
                bitmarkId = bitmarkId,
                isPending = true,
                loadBlock = true,
                loadAsset = true
            ), BiFunction { bitmark, _ -> bitmark })
            else Single.error(e)
        }.ignoreElement()

}