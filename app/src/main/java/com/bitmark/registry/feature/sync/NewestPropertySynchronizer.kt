package com.bitmark.registry.feature.sync

import android.annotation.SuppressLint
import android.util.Log
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
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
        Log.d(TAG, "Starting...")
        compositeDisposable = CompositeDisposable()
        syncNewestPendingBitmarks()
        syncNewestPendingTxs()
    }

    fun stop() {
        compositeDisposable.dispose()
        Log.d(TAG, "Stopped")
    }

    private fun syncNewestPendingBitmarks() {
        compositeDisposable.add(accountRepo.getAccountNumber().flatMap { accountNumber ->
            val offsetStream =
                if (bmOffset == -1L) bitmarkRepo.minStoredPendingBitmarkOffset() else Single.just(
                    bmOffset
                )

            offsetStream.flatMap { offset ->
                Log.d(TAG, "syncNewestPendingBitmarks with offset $offset")
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
                Log.d(TAG, "syncNewestPendingBitmarks exit since reached top")
                return@subscribe
            }
            bmOffset =
                bitmarks.maxBy { b -> b.offset }?.offset ?: return@subscribe
            // retry until nothing new
            syncNewestPendingBitmarks()
        }, { e ->
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

    private fun syncNewestPendingTxs() {

        compositeDisposable.add(accountRepo.getAccountNumber().flatMap { accountNumber ->

            val offsetStream =
                if (txOffset == -1L) bitmarkRepo.minStoredRelevantPendingTxs(
                    accountNumber
                ) else Single.just(txOffset)

            offsetStream.flatMap { offset ->
                Log.d(TAG, "syncNewestPendingTxs with offset $offset")
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
                Log.d(TAG, "syncNewestPendingTxs exit since reached top")
                return@subscribe
            }
            txOffset = txs.maxBy { t -> t.offset }?.offset ?: return@subscribe
            // retry until nothing new
            syncNewestPendingTxs()
        }, { e ->
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