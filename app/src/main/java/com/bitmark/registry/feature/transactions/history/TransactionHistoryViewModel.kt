/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.transactions.history

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.model.entity.AssetClaimingData
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.Constant.OMNISCIENT_ASSET_ID
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.DateTimeUtil.Companion.ISO8601_FORMAT
import com.bitmark.registry.util.DateTimeUtil.Companion.dateToString
import com.bitmark.registry.util.extension.append
import com.bitmark.registry.util.extension.isNetworkError
import com.bitmark.registry.util.livedata.BufferedLiveData
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.TransactionModelView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import java.util.*
import kotlin.Comparator

class TransactionHistoryViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel(lifecycle) {

    companion object {
        const val PAGE_SIZE = 20
    }

    private val listTxsLiveData =
        CompositeLiveData<List<TransactionModelView>>()

    private val refreshTxsLiveData =
        CompositeLiveData<List<TransactionModelView>>()

    private val fetchLatestTxsLiveData =
        CompositeLiveData<List<TransactionModelView>>()

    internal val txsSavedLiveData =
        BufferedLiveData<List<TransactionModelView>>(lifecycle)

    private var currentOffset = -1L

    internal fun listTxsLiveData() = listTxsLiveData.asLiveData()

    internal fun refreshTxsLiveData() = refreshTxsLiveData.asLiveData()

    internal fun fetchLatestTxsLiveData() = fetchLatestTxsLiveData.asLiveData()

    internal fun listTxs() {
        listTxsLiveData.add(rxLiveDataTransformer.single(listTxsStream()))
    }

    private fun listTxsStream(): Single<List<TransactionModelView>> {

        return accountRepo.getAccountNumber().flatMap { accountNumber ->

            val offsetStream =
                if (currentOffset == -1L) {
                    bitmarkRepo.maxStoredRelevantTxOffset(accountNumber)
                } else {
                    Single.just(currentOffset - 1)
                }

            val pendingTxsStream =
                bitmarkRepo.listStoredRelevantPendingTxs(accountNumber)
                    .map { txs -> txs.filterNot { tx -> tx.isDeleteTx() } }

            Single.zip(
                pendingTxsStream,
                offsetStream,
                BiFunction<List<TransactionData>, Long, Triple<String, List<TransactionData>, Long>> { txs, offset ->
                    Triple(accountNumber, txs, offset)
                })

        }.flatMap { t ->

            val owner = t.first
            val storedPendingTxs = t.second // all stored pending txs
            val offset = t.third // offset to query
            val needDedupPendingTxs = storedPendingTxs.isNotEmpty()

            bitmarkRepo.listRelevantTxs(owner, offset, PAGE_SIZE)
                .map { txs -> txs.filterNot { tx -> tx.isDeleteTx() } }
                .map { txs ->
                    val dedupTxs =
                        if (needDedupPendingTxs) txs.filter { b -> b.status != TransactionData.Status.PENDING } else txs
                    if (currentOffset == -1L) {
                        // append all pending txs at the first page
                        val pendingTxs =
                            mutableListOf<TransactionData>().append(
                                storedPendingTxs, dedupTxs
                            )
                        Pair(owner, pendingTxs)
                    } else Pair(owner, dedupTxs)
                }
                .doOnSuccess { p ->
                    val txs = p.second
                    val nonPendingTxs =
                        txs.filter { tx -> tx.status != TransactionData.Status.PENDING }
                    if (nonPendingTxs.isNotEmpty()) {
                        currentOffset =
                            nonPendingTxs.minBy { tx -> tx.offset }!!.offset
                    } else if (txs.isNotEmpty()) {
                        currentOffset =
                            txs.minBy { tx -> tx.offset }!!.offset
                    }
                }

        }.map(mapTxs()).flatMap { txs ->

            // attach claim requests

            val comparator = Comparator<TransactionModelView> { t1, t2 ->
                when {
                    t1.confirmedAt == null && t2.confirmedAt == null -> 0
                    t1.confirmedAt != null && t2.confirmedAt == null -> 1
                    t1.confirmedAt == null && t2.confirmedAt != null -> -1
                    else -> t1.confirmedAt!!.compareTo(t2.confirmedAt!!)
                }

            }

            val maxConfirmedDate = if (currentOffset == -1L) dateToString(
                Date(),
                ISO8601_FORMAT
            ) else txs.maxWith(comparator)?.confirmedAt
            val minConfirmedDate = txs.minWith(comparator)?.confirmedAt
            if (minConfirmedDate != null && maxConfirmedDate != null) {
                listAssetClaimingRequest(
                    OMNISCIENT_ASSET_ID,
                    minConfirmedDate,
                    maxConfirmedDate
                ).map { assetClaim ->
                    if (assetClaim.isEmpty()) txs
                    else {
                        mutableListOf<TransactionModelView>().append(
                            txs,
                            assetClaim
                        ).sortedWith(comparator).reversed()
                    }
                }.onErrorResumeNext { e ->
                    if (e.isNetworkError()) Single.just(txs) else Single.error(e)
                }
            } else {
                Single.just(txs)
            }
        }
    }

    private fun listAssetClaimingRequest(
        assetId: String,
        from: String,
        to: String
    ) = Single.zip(accountRepo.getAccountNumber(),
        bitmarkRepo.listAssetClaimingRequest(
            assetId,
            from,
            to
        ),
        BiFunction<String, List<AssetClaimingData>, List<TransactionModelView>> { accountNumber, claimRequests ->
            claimRequests.map { c ->
                TransactionModelView.newInstance(
                    c,
                    accountNumber
                )
            }
        })

    private fun mapTxs(): (Pair<String, List<TransactionData>>) -> List<TransactionModelView> =
        { p ->
            val owner = p.first
            val txs = p.second

            txs.map { tx ->
                TransactionModelView.newInstance(tx, owner)
            }
        }

    internal fun refreshTxs() {
        refreshTxsLiveData.add(rxLiveDataTransformer.single(fetchTxsStream()))
    }

    internal fun fetchLatestTxs() {
        fetchLatestTxsLiveData.add(rxLiveDataTransformer.single(fetchTxsStream()))
    }

    private fun fetchTxsStream(): Single<List<TransactionModelView>> {
        return accountRepo.getAccountNumber().flatMap { accountNumber ->
            bitmarkRepo.syncLatestRelevantTxs(
                accountNumber
            )
                .map { txs -> txs.filterNot { tx -> tx.isDeleteTx() } }
                .map { txs -> Pair(accountNumber, txs) }
        }.map(mapTxs())
    }

    internal fun reset() {
        currentOffset = -1L
    }

    override fun onCreate() {
        super.onCreate()

        realtimeBus.txsSavedPublisher.subscribe(this) { tx ->
            subscribe(accountRepo.getAccountNumber().map { accountNumber ->
                Pair(
                    accountNumber,
                    listOf(tx)
                )
            }
                .map { p ->
                    val minOffset =
                        p.second.minBy { t -> t.offset }?.offset ?: -1L
                    currentOffset =
                        if (currentOffset == -1L || currentOffset > minOffset) {
                            minOffset
                        } else {
                            currentOffset
                        }
                    mapTxs().invoke(
                        Pair(
                            p.first,
                            p.second.filterNot { tx -> tx.isDeleteTx() })
                    )
                }.observeOn(AndroidSchedulers.mainThread()).subscribe { ts, e ->
                    if (e == null) {
                        txsSavedLiveData.setValue(ts)
                    }
                })
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        super.onDestroy()
    }
}