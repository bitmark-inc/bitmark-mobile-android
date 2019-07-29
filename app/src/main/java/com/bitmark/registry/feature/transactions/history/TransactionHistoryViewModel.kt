package com.bitmark.registry.feature.transactions.history

import androidx.lifecycle.MutableLiveData
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.extension.append
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.TransactionModelView
import io.reactivex.Single
import io.reactivex.functions.BiFunction


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class TransactionHistoryViewModel(
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel() {

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
        MutableLiveData<List<TransactionModelView>>()

    private var currentOffset = -1L

    internal fun listTxsLiveData() = listTxsLiveData.asLiveData()

    internal fun refreshTxsLiveData() = refreshTxsLiveData.asLiveData()

    internal fun fetchLatestTxsLiveData() = fetchLatestTxsLiveData.asLiveData()

    internal fun listTxs() {
        listTxsLiveData.add(rxLiveDataTransformer.single(listTxsStream()))
    }

    private fun listTxsStream(): Single<List<TransactionModelView>> {

        return getAccountNumber().flatMap { accountNumber ->

            val offsetStream =
                if (currentOffset == -1L) bitmarkRepo.maxStoredRelevantTxOffset(
                    accountNumber
                ) else Single.just(
                    currentOffset - 1
                )

            val pendingTxsStream =
                bitmarkRepo.listStoredPendingTxs(accountNumber)

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

        }.map(mapTxs())
    }

    private fun mapTxs(): (Pair<String, List<TransactionData>>) -> List<TransactionModelView> =
        { p ->
            val owner = p.first
            val txs = p.second

            txs.map { tx ->
                TransactionModelView(
                    tx.id,
                    tx.block?.createdAt,
                    tx.owner,
                    tx.previousOwner,
                    tx.asset?.name,
                    tx.status,
                    owner,
                    tx.offset
                )
            }
        }

    internal fun refreshTxs() {
        refreshTxsLiveData.add(rxLiveDataTransformer.single(fetchTxsStream()))
    }

    internal fun fetchLatestTxs() {
        fetchLatestTxsLiveData.add(rxLiveDataTransformer.single(fetchTxsStream()))
    }

    private fun fetchTxsStream(): Single<List<TransactionModelView>> {
        return getAccountNumber().flatMap { accountNumber ->
            Single.zip(
                accountRepo.getAccountInfo(),
                bitmarkRepo.maxStoredRelevantTxOffset(accountNumber),
                BiFunction<Pair<String, Boolean>, Long, Pair<String, Long>> { account, maxOffset ->

                    Pair(account.first, maxOffset)

                })
        }.flatMap { p ->

            val accountNumber = p.first
            val maxOffset = p.second

            bitmarkRepo.syncTxs(
                owner = accountNumber,
                at = maxOffset + 1,
                to = "later",
                limit = PAGE_SIZE,
                sent = true,
                isPending = true
            ).map { bitmarks -> Pair(accountNumber, bitmarks) }

        }.map(mapTxs())
    }

    private fun getAccountNumber() =
        accountRepo.getAccountInfo().map { a -> a.first }

    internal fun reset() {
        currentOffset = -1L
    }

    override fun onCreate() {
        super.onCreate()
        realtimeBus.txsSavedPublisher.subscribe(this) { txs ->
            subscribe(getAccountNumber().map { accountNumber ->
                Pair(
                    accountNumber,
                    txs
                )
            }
                .map { p ->
                    val minOffset =
                        p.second.minBy { t -> t.offset }?.offset ?: -1L
                    currentOffset =
                        if (currentOffset == -1L || currentOffset > minOffset) minOffset else currentOffset
                    mapTxs().invoke(p)
                }.subscribe { ts, e ->
                    if (e == null) {
                        txsSavedLiveData.set(ts)
                    }
                })
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        super.onDestroy()
    }
}