package com.bitmark.registry.feature.sync

import android.annotation.SuppressLint
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.logging.Tracer
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.CompositeException
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-08-28
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 *
 */
@SuppressLint("LongLogTag")
class NewestPropertySynchronizer @Inject constructor(
    private val bitmarkRepo: BitmarkRepository,
    private val accountRepo: AccountRepository
) {

    companion object {
        private const val TAG = "NewestPropertySynchronizer"

        private const val ITEM_PER_PAGE = 100
    }

    private lateinit var compositeDisposable: CompositeDisposable

    private var bmOffset = -1L

    private var txOffset = -1L

    fun start() {
        Tracer.DEBUG.log(TAG, "Starting...")
        compositeDisposable = CompositeDisposable()
        syncNewestPendingBitmarks()
        syncNewestPendingTxs()
    }

    fun stop() {
        compositeDisposable.dispose()
        Tracer.DEBUG.log(TAG, "Stopped")
    }

    private fun syncNewestPendingBitmarks() {
        compositeDisposable.add(accountRepo.getAccountNumber().flatMap { accountNumber ->
            val offsetStream =
                if (bmOffset == -1L) bitmarkRepo.minStoredPendingBitmarkOffset() else Single.just(
                    bmOffset
                )

            offsetStream.flatMap { offset ->
                Tracer.DEBUG.log(TAG, "syncNewestPendingBitmarks with offset $offset")
                if (offset == -1L) {
                    Single.just(listOf())
                } else {
                    bitmarkRepo.syncBitmarks(
                        owner = accountNumber,
                        at = offset,
                        to = "later",
                        pending = true,
                        loadAsset = true,
                        limit = ITEM_PER_PAGE
                    )
                }
            }

        }.subscribe({ bitmarks ->
            if (bitmarks.size < ITEM_PER_PAGE) {
                Tracer.DEBUG.log(TAG, "syncNewestPendingBitmarks exit since reached top")
                return@subscribe
            }
            bmOffset =
                bitmarks.maxBy { b -> b.offset }?.offset ?: return@subscribe
            // retry until nothing new
            syncNewestPendingBitmarks()
        }, { e ->
            if (e is CompositeException) {
                e.exceptions.forEach { ex ->
                    Tracer.ERROR.log(
                        TAG,
                        "${ex.javaClass}-${ex.message}"
                    )
                }
            } else {
                Tracer.ERROR.log(TAG, "${e.javaClass}-${e.message}")
            }
        }))
    }

    private fun syncNewestPendingTxs() {

        compositeDisposable.add(accountRepo.getAccountNumber().flatMap { accountNumber ->

            val offsetStream =
                if (txOffset == -1L) bitmarkRepo.minStoredRelevantPendingTxs(
                    accountNumber
                ) else Single.just(txOffset)

            offsetStream.flatMap { offset ->
                Tracer.DEBUG.log(TAG, "syncNewestPendingTxs with offset $offset")
                if (offset == -1L) {
                    Single.just(listOf())
                } else {
                    bitmarkRepo.syncTxs(
                        owner = accountNumber,
                        at = offset,
                        to = "later",
                        sent = true,
                        isPending = true,
                        limit = ITEM_PER_PAGE
                    )
                }
            }

        }.subscribe({ txs ->
            if (txs.size < ITEM_PER_PAGE) {
                Tracer.DEBUG.log(TAG, "syncNewestPendingTxs exit since reached top")
                return@subscribe
            }
            txOffset = txs.maxBy { t -> t.offset }?.offset ?: return@subscribe
            // retry until nothing new
            syncNewestPendingTxs()
        }, { e ->
            if (e is CompositeException) {
                e.exceptions.forEach { ex ->
                    Tracer.ERROR.log(
                        TAG,
                        "${ex.javaClass}-${ex.message}"
                    )
                }
            } else {
                Tracer.ERROR.log(TAG, "${e.javaClass}-${e.message}")
            }
        }))
    }
}