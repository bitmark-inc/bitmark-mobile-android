package com.bitmark.registry.feature.sync

import android.util.Log
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.util.extension.isDbRecNotFoundError
import com.bitmark.registry.util.extension.poll
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.functions.BiFunction
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
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

        private const val CONCURRENT_TASKS_COUNT = 5
    }

    private val compositeDisposable = CompositeDisposable()

    private val taskQueue = ArrayDeque<Completable>()

    private val isProcessing = AtomicBoolean(false)

    fun start() {
        wsEventBus.bitmarkChangedPublisher.subscribe(this) { m ->
            val bitmarkId = m["bitmark_id"] as String
            val presence = m["presence"] as Boolean
            process(processBitmarkChangedEvent(bitmarkId, presence))
        }

        wsEventBus.newPendingTxPublisher.subscribe(this) { m ->
            val owner = m["owner"] ?: return@subscribe
            val previousTxId = m["prev_tx_id"] ?: return@subscribe
            process(processNewPendingTxEvent(owner, previousTxId))
        }

        wsEventBus.newPendingIssuancePublisher.subscribe(this) { bitmarkId ->
            process(processNewPendingIssuance(bitmarkId))
        }
    }

    fun stop() {
        compositeDisposable.dispose()
    }

    private fun process(task: Completable) {
        if (isProcessing.get()) {
            taskQueue.add(task)
        } else {
            subscribe(task.doOnSubscribe { isProcessing.set(true) }
                .doAfterTerminate {
                    isProcessing.set(false)
                    execute()
                }
                .doOnDispose { isProcessing.set(false) }
                .subscribe({}, { e ->
                    if (e is CompositeException) {
                        e.exceptions.forEach { ex ->
                            Log.e(
                                TAG,
                                "${ex.javaClass}-${ex.message}"
                            )
                        }
                    } else {
                        Log.e(TAG, "${e.javaClass}-${e.message}")
                    }
                }))
        }
    }

    private fun execute() {
        if (taskQueue.isEmpty()) return
        process(
            Completable.mergeDelayError(
                taskQueue.poll(
                    CONCURRENT_TASKS_COUNT
                )
            )
        )
    }

    private fun processBitmarkChangedEvent(
        bitmarkId: String,
        presence: Boolean
    ) = getAccountNumber().flatMap { accountNumber ->
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
    }.flatMapCompletable {
        if (presence) bitmarkRepo.syncBitmark(
            bitmarkId,
            true
        ).ignoreElement() else Completable.complete()
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


        return getAccountNumber().flatMapCompletable { accountNumber ->
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

    private fun subscribe(disposable: Disposable) =
        compositeDisposable.add(disposable)

    private fun getAccountNumber() =
        accountRepo.getAccountInfo().map { a -> a.first }
}