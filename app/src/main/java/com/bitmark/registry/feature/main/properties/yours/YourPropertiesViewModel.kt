package com.bitmark.registry.feature.main.properties.yours

import androidx.lifecycle.MutableLiveData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.BitmarkData.Status.ISSUING
import com.bitmark.registry.data.model.BitmarkData.Status.TRANSFERRING
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.extension.append
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.BitmarkModelView
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-07-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class YourPropertiesViewModel(
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel() {

    companion object {
        private const val PAGE_SIZE = 50
    }

    internal val deletedBitmarkLiveData = MutableLiveData<List<String>>()

    private val listBitmarksLiveData =
        CompositeLiveData<List<BitmarkModelView>>()

    private val fetchBitmarksLiveData =
        CompositeLiveData<List<BitmarkModelView>>()

    private val markSeenLiveData = CompositeLiveData<String>()

    private var currentOffset = -1L

    fun reset() {
        currentOffset = -1L
    }

    internal fun listBitmarksLiveData() = listBitmarksLiveData.asLiveData()

    internal fun fetchBitmarksLiveData() = fetchBitmarksLiveData.asLiveData()

    internal fun markSeenLiveData() = markSeenLiveData.asLiveData()

    internal fun listBitmark() =
        listBitmarksLiveData.add(rxLiveDataTransformer.maybe(listBitmarkStream()))

    internal fun markSeen(bitmarkId: String) = markSeenLiveData.add(
        rxLiveDataTransformer.single(
            bitmarkRepo.markBitmarkSeen(bitmarkId)
        )
    )

    private fun listBitmarkStream(): Maybe<List<BitmarkModelView>> {

        val offsetStream =
            if (currentOffset == -1L) bitmarkRepo.maxStoredBitmarkOffset() else Single.just(
                currentOffset - 1
            )

        val pendingBitmarksStream =
            bitmarkRepo.listStoredPendingBitmarks().toSingle()

        return Single.zip(
            pendingBitmarksStream,
            accountRepo.getAccountInfo(),
            offsetStream,
            Function3<List<BitmarkData>, Pair<String, Boolean>, Long, Triple<List<BitmarkData>, String, Long>> { storedPendingBitmarks, account, offset ->
                Triple(storedPendingBitmarks, account.first, offset)

            }).flatMapMaybe { t ->

            val storedPendingBitmarks = t.first // all stored pending bitmark
            val accountNumber = t.second
            val offset = t.third // offset to query
            val needDedupPendingBitmarks = storedPendingBitmarks.isNotEmpty()

            bitmarkRepo.listBitmarks(accountNumber, offset, PAGE_SIZE)
                .map { bitmarks ->
                    val dedupBitmarks =
                        if (needDedupPendingBitmarks) bitmarks.filter { b -> b.status != TRANSFERRING && b.status != ISSUING } else bitmarks
                    if (currentOffset == -1L) {
                        // append all pending bitmark at the first page
                        mutableListOf<BitmarkData>().append(
                            storedPendingBitmarks, dedupBitmarks
                        )
                    } else dedupBitmarks
                }
                .map { bitmarks -> Pair(accountNumber, bitmarks) }
                .doOnSuccess { p ->
                    val bitmarks = p.second
                    val nonPendingBitmarks =
                        bitmarks.filter { b -> b.status != TRANSFERRING && b.status != ISSUING }
                    if (nonPendingBitmarks.isNotEmpty()) {
                        currentOffset =
                            nonPendingBitmarks.minBy { b -> b.offset }!!.offset
                    } else if (bitmarks.isNotEmpty()) {
                        currentOffset =
                            bitmarks.minBy { b -> b.offset }!!.offset
                    }
                }

        }.flatMapSingle(checkAssetFileStream()).map(bitmarkMapFunc()).toMaybe()

    }

    internal fun fetchBitmarks() {
        fetchBitmarksLiveData.add(
            rxLiveDataTransformer.maybe(
                fetchBitmarksStream()
            )
        )
    }

    private fun fetchBitmarksStream(): Maybe<List<BitmarkModelView>> {
        return Single.zip(
            accountRepo.getAccountInfo(),
            bitmarkRepo.maxStoredBitmarkOffset(),
            BiFunction<Pair<String, Boolean>, Long, Pair<String, Long>> { account, maxOffset ->

                Pair(account.first, maxOffset)

            }).flatMap { p ->

            val accountNumber = p.first
            val maxOffset = p.second

            bitmarkRepo.syncBitmarks(
                owner = accountNumber,
                at = maxOffset + 1,
                to = "later",
                limit = PAGE_SIZE,
                pending = true
            ).doOnSuccess { bitmarks ->
                if (bitmarks.isNotEmpty())
                    currentOffset = bitmarks.minBy { b -> b.offset }!!.offset
            }.map { bitmarks -> Pair(accountNumber, bitmarks) }

        }.flatMap(checkAssetFileStream()).map(bitmarkMapFunc()).toMaybe()
    }

    private fun checkAssetFileStream(): (Pair<String, List<BitmarkData>>) -> Single<Pair<String, List<BitmarkData>>> =
        { p ->
            // go to storage and check file corresponding to asset
            val accountNumber = p.first
            val bitmarks = p.second

            if (bitmarks.isNullOrEmpty()) {
                Single.just(Pair(accountNumber, listOf()))
            } else {
                val checkAssetFileStreams =
                    mutableListOf<Single<Pair<String, File?>>>()
                bitmarks.forEach { b ->

                    val checkAssetFileStream =
                        bitmarkRepo.checkAssetFile(accountNumber, b.assetId)
                            .doOnSuccess { p ->

                                val assetId = p.first
                                val file = p.second
                                bitmarks.filter { b -> b.assetId == assetId }
                                    .forEach { b ->
                                        b.asset?.file = file
                                    }
                            }

                    checkAssetFileStreams.add(checkAssetFileStream)
                }
                Single.merge(checkAssetFileStreams).lastOrError()
                    .map { Pair(accountNumber, bitmarks) }
            }
        }

    private fun bitmarkMapFunc(): (Pair<String, List<BitmarkData>>) -> List<BitmarkModelView> =
        { p ->
            val accountNumber = p.first
            val bitmarks = p.second
            bitmarks.map { b ->
                BitmarkModelView.newInstance(b, accountNumber)
            }
        }

    override fun onCreate() {
        super.onCreate()
        realtimeBus.bitmarkDeletedPublisher.subscribe(this) { bitmarkIds ->
            deletedBitmarkLiveData.value = bitmarkIds
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}