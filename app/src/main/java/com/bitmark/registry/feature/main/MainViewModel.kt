package com.bitmark.registry.feature.main

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import io.reactivex.Maybe


/**
 * @author Hieu Pham
 * @since 2019-07-27
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MainViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val wsEventBus: WebSocketEventBus
) :
    BaseViewModel(lifecycle) {

    override fun onCreate() {
        super.onCreate()

        wsEventBus.bitmarkChangedPublisher.subscribe(this) { m ->
            val bitmarkId = m["bitmark_id"] as String
            val presence = m["presence"] as Boolean
            processBitmarkChangedEvent(bitmarkId, presence)
        }

        wsEventBus.newPendingTxPublisher.subscribe(this) { m ->
            val owner = m["owner"]
            processNewPendingTxEvent(owner!!)
        }
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

    private fun processNewPendingTxEvent(owner: String) {
        subscribe(
            getAccountNumber().flatMapMaybe { accountNumber ->
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
                    }.flatMapMaybe {
                        if (owner != accountNumber) {
                            // outgoing tx
                            Maybe.empty()
                        } else {
                            // incoming tx, sync to get latest bitmark
                            bitmarkRepo.maxStoredBitmarkOffset()
                                .flatMap { offset ->
                                    bitmarkRepo.syncBitmarks(
                                        owner = accountNumber,
                                        at = offset,
                                        to = "later",
                                        pending = true,
                                        loadAsset = true
                                    )
                                }.toMaybe()
                        }
                    }
            }.subscribe(
                {},
                {})
        )
    }

    private fun getAccountNumber() =
        accountRepo.getAccountInfo().map { p -> p.first }

    override fun onDestroy() {
        wsEventBus.disconnect()
        super.onDestroy()
    }
}