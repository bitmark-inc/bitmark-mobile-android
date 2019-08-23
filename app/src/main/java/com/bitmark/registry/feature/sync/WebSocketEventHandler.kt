package com.bitmark.registry.feature.sync

import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.util.extension.isDbRecNotFoundError
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
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

    private val compositeDisposable = CompositeDisposable()

    fun start() {
        wsEventBus.bitmarkChangedPublisher.subscribe(this) { m ->
            val bitmarkId = m["bitmark_id"] as String
            val presence = m["presence"] as Boolean
            processBitmarkChangedEvent(bitmarkId, presence)
        }

        wsEventBus.newPendingTxPublisher.subscribe(this) { m ->
            val owner = m["owner"] ?: return@subscribe
            val previousTxId = m["prev_tx_id"] ?: return@subscribe
            processNewPendingTxEvent(owner, previousTxId)
        }

        wsEventBus.newPendingIssuancePublisher.subscribe(this) { bitmarkId ->
            processNewPendingIssuance(bitmarkId)
        }
    }

    fun stop() {
        compositeDisposable.dispose()
    }

    private fun processBitmarkChangedEvent(
        bitmarkId: String,
        presence: Boolean
    ) {
        val stream = getAccountNumber().flatMap { accountNumber ->
            bitmarkRepo.maxStoredRelevantTxOffset(accountNumber)
                .flatMap { offset ->
                    bitmarkRepo.syncTxs(
                        owner = accountNumber,
                        sent = true,
                        bitmarkId = bitmarkId,
                        loadAsset = true,
                        loadBlock = true,
                        at = offset,
                        to = "later"
                    )
                }
        }.flatMapMaybe {
            if (presence) bitmarkRepo.syncBitmark(
                bitmarkId,
                true
            ).toMaybe() else Maybe.empty()
        }
        subscribe(stream.subscribe({}, {}))
    }

    private fun processNewPendingTxEvent(owner: String, prevTxId: String) {

        val deleteBmStream =
            fun(accountNumber: String) =
                bitmarkRepo.getStoredTxById(prevTxId).flatMapCompletable { tx ->
                    bitmarkRepo.deleteStoredBitmark(
                        accountNumber,
                        tx.bitmarkId,
                        tx.assetId
                    )
                }


        subscribe(
            getAccountNumber().flatMapCompletable { accountNumber ->
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
            }.subscribe(
                {},
                {})
        )
    }

    private fun processNewPendingIssuance(bitmarkId: String) {
        subscribe(bitmarkRepo.getStoredBitmarkById(bitmarkId).onErrorResumeNext { e ->
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
        }.subscribe({}, {}))
    }

    private fun subscribe(disposable: Disposable) =
        compositeDisposable.add(disposable)

    private fun getAccountNumber() =
        accountRepo.getAccountInfo().map { a -> a.first }
}