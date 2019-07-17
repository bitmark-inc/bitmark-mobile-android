package com.bitmark.registry.feature.property_detail

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.TransactionModelView
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.functions.BiFunction


/**
 * @author Hieu Pham
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertyDetailViewModel(
    private val bitmarkRepo: BitmarkRepository,
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel() {

    private val getProvenanceLiveData =
        CompositeLiveData<Pair<String, List<TransactionModelView>>>()

    internal fun getProvenanceLiveData() = getProvenanceLiveData.asLiveData()

    private val syncProvenanceLiveData =
        CompositeLiveData<Pair<String, List<TransactionModelView>>>()

    internal fun syncProvenanceLiveData() = syncProvenanceLiveData.asLiveData()

    internal fun getProvenance(bitmarkId: String) {
        getProvenanceLiveData.add(
            rxLiveDataTransformer.maybe(
                getProvenanceStream(bitmarkId)
            )
        )
    }

    internal fun syncProvenance(bitmarkId: String) {
        syncProvenanceLiveData.add(
            rxLiveDataTransformer.single(
                syncProvenanceStream(bitmarkId)
            )
        )
    }

    private fun getProvenanceStream(bitmarkId: String): Maybe<Pair<String, List<TransactionModelView>>> {
        val accountStream =
            accountRepo.getAccountInfo().map { a -> a.first }.toMaybe()
        val txsStream = bitmarkRepo.listTxs(
            bitmarkId = bitmarkId,
            loadBlock = true,
            isPending = true
        ).map { txs ->
            txs.map { tx ->
                TransactionModelView(
                    tx.block?.createdAt ?: "",
                    tx.owner,
                    tx.status
                )
            }
        }

        return Maybe.zip(
            accountStream,
            txsStream,
            BiFunction { accountNumber, txs ->
                Pair(
                    accountNumber,
                    txs
                )
            })
    }

    private fun syncProvenanceStream(bitmarkId: String): Single<Pair<String, List<TransactionModelView>>> {
        val accountStream =
            accountRepo.getAccountInfo().map { a -> a.first }
        val txsStream = bitmarkRepo.syncTxs(
            bitmarkId = bitmarkId,
            loadBlock = true,
            isPending = true,
            loadAsset = true
        ).map { txs ->
            txs.map { tx ->
                TransactionModelView(
                    tx.block?.createdAt ?: "",
                    tx.owner,
                    tx.status
                )
            }
        }

        return Single.zip(
            accountStream,
            txsStream,
            BiFunction { accountNumber, txs ->
                Pair(
                    accountNumber,
                    txs
                )
            })
    }

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }

}