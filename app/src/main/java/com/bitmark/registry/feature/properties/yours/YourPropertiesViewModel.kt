package com.bitmark.registry.feature.properties.yours

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.BitmarkData.Status.ISSUING
import com.bitmark.registry.data.model.BitmarkData.Status.TRANSFERRING
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.extension.append
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.BufferedLiveData
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.BitmarkModelView
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-07-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class YourPropertiesViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel(lifecycle) {

    companion object {
        private const val PAGE_SIZE = 20
    }

    internal val deletedBitmarkLiveData =
        BufferedLiveData<String>(lifecycle)

    internal val bitmarkSavedLiveData =
        BufferedLiveData<List<BitmarkModelView>>(lifecycle)

    private val listBitmarksLiveData =
        CompositeLiveData<List<BitmarkModelView>>()

    private val refreshBitmarksLiveData =
        CompositeLiveData<List<BitmarkModelView>>()

    private val markSeenLiveData = CompositeLiveData<String>()

    private val fetchLatestBitmarksLiveData =
        CompositeLiveData<List<BitmarkModelView>>()

    internal val assetFileSavedLiveData =
        BufferedLiveData<Pair<String, File>>(lifecycle)

    internal val assetTypeChangedLiveData =
        BufferedLiveData<Pair<String, AssetData.Type>>(lifecycle)

    private var currentOffset = -1L

    fun reset() {
        currentOffset = -1L
    }

    internal fun listBitmarksLiveData() = listBitmarksLiveData.asLiveData()

    internal fun refreshBitmarksLiveData() =
        refreshBitmarksLiveData.asLiveData()

    internal fun markSeenLiveData() = markSeenLiveData.asLiveData()

    internal fun fetchLatestBitmarksLiveData() =
        fetchLatestBitmarksLiveData.asLiveData()

    internal fun listBitmark() =
        listBitmarksLiveData.add(
            rxLiveDataTransformer.maybe(
                listBitmarkStream()
            )
        )

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

        val pendingBitmarksStream = accountRepo.getAccountNumber()
            .flatMap { accountNumber ->
                bitmarkRepo.listStoredPendingBitmarks(accountNumber)
                    .map { bitmarks -> Pair(accountNumber, bitmarks) }
            }

        return Single.zip(
            pendingBitmarksStream,
            offsetStream,
            BiFunction<Pair<String, List<BitmarkData>>, Long, Triple<String, List<BitmarkData>, Long>> { p, offset ->
                Triple(p.first, p.second, offset)

            }).flatMap { t ->

            val accountNumber = t.first
            val storedPendingBitmarks = t.second // all stored pending bitmark
            val offset = t.third // offset to query
            val needDedupPendingBitmarks = storedPendingBitmarks.isNotEmpty()

            bitmarkRepo.listBitmarks(accountNumber, offset, PAGE_SIZE)
                .map { bitmarks ->
                    val dedupBitmarks =
                        if (needDedupPendingBitmarks) bitmarks.filter { b -> b.status != TRANSFERRING && b.status != ISSUING } else bitmarks
                    if (currentOffset == -1L) {
                        // append all pending bitmark at the first page
                        val pendingBitmarks =
                            mutableListOf<BitmarkData>().append(
                                storedPendingBitmarks, dedupBitmarks
                            )
                        Pair(accountNumber, pendingBitmarks)
                    } else Pair(accountNumber, dedupBitmarks)
                }
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

        }.flatMap(checkAssetFileStream())
            .map(bitmarkMapFunc()).toMaybe()

    }

    internal fun refreshBitmarks() {
        refreshBitmarksLiveData.add(
            rxLiveDataTransformer.maybe(
                fetchBitmarksStream()
            )
        )
    }

    internal fun fetchLatestBitmarks() {
        fetchLatestBitmarksLiveData.add(
            rxLiveDataTransformer.maybe(
                fetchBitmarksStream()
            )
        )
    }

    private fun fetchBitmarksStream(): Maybe<List<BitmarkModelView>> {
        return accountRepo.getAccountNumber()
            .flatMap { accountNumber ->
                bitmarkRepo.syncLatestBitmarks(accountNumber)
                    .map { bitmarks -> Pair(accountNumber, bitmarks) }
            }.flatMap(checkAssetFileStream())
            .map(bitmarkMapFunc()).toMaybe()
    }

    private fun checkAssetFileStream(): (Pair<String, List<BitmarkData>>) -> Single<Pair<String, List<BitmarkData>>> =
        { p ->

            val accountNumber = p.first
            val bitmarks = p.second

            if (bitmarks.isNullOrEmpty()) {
                Single.just(Pair(accountNumber, listOf()))
            } else {
                val checkAssetFileStreams =
                    mutableListOf<Single<Pair<String, File?>>>()

                bitmarks.forEach { b ->
                    checkAssetFileStreams.add(
                        bitmarkRepo.checkAssetFile(
                            accountNumber,
                            b.assetId
                        )
                    )
                }
                Single.merge(checkAssetFileStreams)
                    .collectInto(
                        mutableListOf<Pair<String, File?>>(),
                        { c, p -> c.add(p) }).map { collection ->
                        collection.forEach { p ->
                            bitmarks.firstOrNull { b -> b.asset?.id == p.first }
                                ?.asset?.file = p.second
                        }
                        Pair(accountNumber, bitmarks)
                    }
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
        realtimeBus.bitmarkDeletedPublisher.subscribe(this) { p ->
            val bitmarkId = p.first
            deletedBitmarkLiveData.set(bitmarkId)
        }

        realtimeBus.assetFileSavedPublisher.subscribe(this) { p ->
            assetFileSavedLiveData.set(p)
        }

        realtimeBus.assetTypeChangedPublisher.subscribe(this) { p ->
            assetTypeChangedLiveData.set(p)
        }

        realtimeBus.bitmarkSavedPublisher.subscribe(this) { bitmark ->
            subscribe(accountRepo.getAccountNumber().map { accountNumber ->
                Pair(
                    accountNumber,
                    listOf(bitmark)
                )
            }.flatMap(checkAssetFileStream()).map { p ->
                val minOffset = p.second.minBy { b -> b.offset }?.offset ?: -1L
                currentOffset =
                    if (currentOffset == -1L || currentOffset > minOffset) {
                        minOffset
                    } else {
                        currentOffset
                    }
                bitmarkMapFunc().invoke(p)
            }.observeOn(AndroidSchedulers.mainThread()).subscribe { b, e ->
                if (e == null) {
                    bitmarkSavedLiveData.set(b)
                }
            })
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}