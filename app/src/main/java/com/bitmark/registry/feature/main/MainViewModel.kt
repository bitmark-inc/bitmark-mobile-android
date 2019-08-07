package com.bitmark.registry.feature.main

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.util.livedata.BufferedLiveData
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.BitmarkModelView
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction


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
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val wsEventBus: WebSocketEventBus,
    private val realtimeBus: RealtimeBus
) :
    BaseViewModel(lifecycle) {

    internal val checkBitmarkSeenLiveData = BufferedLiveData<Boolean>(lifecycle)

    internal val checkActionRequiredLiveData =
        BufferedLiveData<Int>(lifecycle)

    private val getBitmarkLiveData = CompositeLiveData<BitmarkModelView>()

    internal fun getBitmarkLiveData() = getBitmarkLiveData.asLiveData()

    internal fun getBitmark(bitmarkId: String) {
        getBitmarkLiveData.add(
            rxLiveDataTransformer.single(
                Single.zip(
                    accountRepo.getAccountInfo().map { a -> a.first },
                    bitmarkRepo.syncBitmark(
                        bitmarkId, true
                    ),
                    BiFunction { accountNumber, bitmark ->
                        BitmarkModelView.newInstance(
                            bitmark,
                            accountNumber
                        )
                    }
                )
            )
        )
    }

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

        realtimeBus.bitmarkSeenPublisher.subscribe(this) {
            checkUnseenBitmark()
        }

        realtimeBus.bitmarkDeletedPublisher.subscribe(this) {
            checkUnseenBitmark()
        }

        realtimeBus.bitmarkSavedPublisher.subscribe(this) {
            checkUnseenBitmark()
        }

        realtimeBus.actionRequiredDeletedPublisher.subscribe(this) {
            checkActionRequired()
        }
    }

    internal fun checkUnseenBitmark() {
        subscribe(
            accountRepo.getAccountInfo().map { a -> a.first }.flatMap { accountNumber ->
                bitmarkRepo.checkUnseenBitmark(
                    accountNumber
                )
            }
                .observeOn(
                    AndroidSchedulers.mainThread()
                ).subscribe { has, e ->
                    if (e == null) {
                        checkBitmarkSeenLiveData.setValue(has)
                    }
                })
    }

    internal fun checkActionRequired() {
        subscribe(
            accountRepo.getActionRequired().map { actions -> actions.size }.observeOn(
                AndroidSchedulers.mainThread()
            ).subscribe { count, e ->
                if (e == null) {
                    checkActionRequiredLiveData.setValue(count)
                }
            })
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
        realtimeBus.unsubscribe(this)
        wsEventBus.disconnect()
        super.onDestroy()
    }
}