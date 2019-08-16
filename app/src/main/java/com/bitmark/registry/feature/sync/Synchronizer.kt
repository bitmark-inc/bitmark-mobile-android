package com.bitmark.registry.feature.sync

import android.util.Log
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.Constant
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable


/**
 * @author Hieu Pham
 * @since 2019-08-16
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class Synchronizer(
    private val bitmarkRepo: BitmarkRepository,
    private val accountRepo: AccountRepository
) {

    companion object {
        private const val TAG = "Synchronizer"

        private const val ITEM_PER_PAGE = 100
    }

    private val compositeDisposable = CompositeDisposable()

    private var minBitmarkOffset = -1L

    private var minTxsOffset = -1L

    fun start() {
        Log.d(TAG, "Started")
        syncBitmarks()
        syncTxs()
        syncClaimRequests()
    }

    fun stop() {
        compositeDisposable.dispose()
        Log.d(TAG, "Stopped")
    }

    private fun syncBitmarks() {

        compositeDisposable.add(getAccountNumber().flatMap { accountNumber ->
            val offsetStream =
                if (minBitmarkOffset == -1L) bitmarkRepo.minStoredBitmarkOffset() else Single.just(
                    minBitmarkOffset
                )

            offsetStream.flatMap { offset ->
                bitmarkRepo.syncBitmarks(
                    owner = accountNumber,
                    at = offset,
                    to = "earlier",
                    pending = true,
                    loadAsset = true,
                    limit = ITEM_PER_PAGE
                )
            }
        }.retry(1).subscribe { bitmarks, e ->
            if (e == null) {
                minBitmarkOffset =
                    bitmarks.minBy { b -> b.offset }?.offset ?: return@subscribe
                Log.d(TAG, "minBitmarkOffset: $minBitmarkOffset")
                if (bitmarks.size == ITEM_PER_PAGE) {
                    Log.d(TAG, "resync bitmarks recursively")
                    syncBitmarks()
                } else {
                    Log.d(TAG, "stop sync bitmarks since fetched all")
                }
            }
        })
    }

    private fun syncTxs() {

        compositeDisposable.add(getAccountNumber().flatMap { accountNumber ->
            val offsetStream =
                if (minTxsOffset == -1L) bitmarkRepo.minStoredRelevantTxOffset(
                    accountNumber
                ) else Single.just(
                    minTxsOffset
                )

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
            }
        }.retry(1).subscribe { txs, e ->
            if (e == null) {
                minTxsOffset =
                    txs.minBy { t -> t.offset }?.offset ?: return@subscribe
                Log.d(TAG, "minTxsOffset: $minTxsOffset")
                if (txs.size == ITEM_PER_PAGE) {
                    Log.d(TAG, "resync txs recursively")
                    syncTxs()
                } else {
                    Log.d(TAG, "stop sync txs since fetched all")
                }
            }
        })
    }

    private fun syncClaimRequests() {
        compositeDisposable.add(
            bitmarkRepo.syncAssetClaimingRequest(Constant.OMNISCIENT_ASSET_ID).retry(
                1
            ).subscribe { _, _ ->
                Log.d(TAG, "sync claim request done")
            })
    }

    private fun getAccountNumber() =
        accountRepo.getAccountInfo().map { a -> a.first }
}