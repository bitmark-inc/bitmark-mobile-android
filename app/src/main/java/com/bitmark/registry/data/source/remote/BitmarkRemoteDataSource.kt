package com.bitmark.registry.data.source.remote

import com.bitmark.apiservice.params.TransferParams
import com.bitmark.apiservice.params.query.BitmarkQueryBuilder
import com.bitmark.apiservice.params.query.TransactionQueryBuilder
import com.bitmark.apiservice.response.GetBitmarksResponse
import com.bitmark.apiservice.response.GetTransactionsResponse
import com.bitmark.apiservice.utils.callback.Callback1
import com.bitmark.apiservice.utils.error.UnexpectedException
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.BlockData
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.service.CoreApi
import com.bitmark.registry.data.source.remote.api.service.FileCourierServerApi
import com.bitmark.registry.data.source.remote.api.service.MobileServerApi
import com.bitmark.sdk.features.Bitmark
import com.bitmark.sdk.features.Transaction
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BitmarkRemoteDataSource @Inject constructor(
    coreApi: CoreApi, mobileServerApi: MobileServerApi,
    fileCourierServerApi: FileCourierServerApi, converter: Converter
) : RemoteDataSource(
    coreApi, mobileServerApi, fileCourierServerApi, converter
) {

    fun listBitmarks(
        owner: String? = null,
        bitmarkIds: List<String>? = null,
        at: Long = 0,
        to: String = "earlier",
        limit: Int = 100,
        pending: Boolean = false,
        issuer: String? = null,
        refAssetId: String? = null,
        loadAsset: Boolean = false
    ): Single<Pair<List<BitmarkData>, List<AssetData>>> {
        return Single.create(SingleOnSubscribe<Pair<List<BitmarkData>, List<AssetData>>> { emitter ->
            val builder =
                BitmarkQueryBuilder().limit(limit).pending(pending)
                    .loadAsset(loadAsset)
            if (at > 0) builder.at(at).to(to)
            if (null != issuer) builder.issuedBy(issuer)
            if (null != owner) builder.ownedBy(owner)
            if (null != refAssetId) builder.referencedAsset(refAssetId)
            if (null != bitmarkIds) builder.bitmarkIds(bitmarkIds.toTypedArray())
            Bitmark.list(builder, object : Callback1<GetBitmarksResponse> {
                override fun onSuccess(data: GetBitmarksResponse) {
                    emitter.onSuccess(
                        Pair(
                            if (data.bitmarks.isNullOrEmpty()) listOf() else data.bitmarks.map(
                                converter.mapBitmark()
                            ),
                            if (data.assets.isNullOrEmpty()) listOf() else data.assets.map(
                                converter.mapAsset()
                            )
                        )
                    )
                }

                override fun onError(throwable: Throwable) {
                    emitter.onError(throwable)
                }

            })
        }).subscribeOn(Schedulers.io())
    }

    fun listTxs(
        owner: String? = null,
        assetId: String? = null,
        bitmarkId: String? = null,
        loadAsset: Boolean = false,
        blockNumber: Long? = null,
        isPending: Boolean? = false,
        sent: Boolean = false,
        at: Long? = null,
        to: String? = null,
        limit: Int = 100,
        loadBlock: Boolean = false
    ): Single<Triple<List<TransactionData>, List<AssetData>, List<BlockData>>> =
        Single.create(
            SingleOnSubscribe<Triple<List<TransactionData>, List<AssetData>, List<BlockData>>> { emt ->

                val queryBuilder =
                    TransactionQueryBuilder().loadAsset(loadAsset)
                        .loadBlock(loadBlock).pending(isPending).limit(limit)
                if (!owner.isNullOrEmpty()) {
                    queryBuilder.ownedBy(owner)
                    if (sent) queryBuilder.ownedByWithTransient(owner)
                }
                if (!assetId.isNullOrEmpty()) queryBuilder.referencedAsset(
                    assetId
                )
                if (!bitmarkId.isNullOrEmpty()) queryBuilder.referencedBitmark(
                    bitmarkId
                )
                if (blockNumber != null) queryBuilder.referencedBlockNumber(
                    blockNumber
                )
                if (at != null && to != null) queryBuilder.at(at).to(to)

                Transaction.list(
                    queryBuilder,
                    object : Callback1<GetTransactionsResponse> {
                        override fun onSuccess(data: GetTransactionsResponse?) {
                            if (data == null) emt.onError(UnexpectedException("response is null"))
                            else emt.onSuccess(
                                Triple(
                                    if (data.transactions.isNullOrEmpty()) listOf() else data.transactions.map(
                                        converter.mapTx()
                                    ),
                                    if (data.assets.isNullOrEmpty()) listOf() else data.assets.map(
                                        converter.mapAsset()
                                    ),
                                    if (data.blocks.isNullOrEmpty()) listOf() else data.blocks.map(
                                        converter.mapBlk()
                                    )
                                )
                            )
                        }

                        override fun onError(throwable: Throwable?) {
                            emt.onError(throwable!!)
                        }

                    })

            }).subscribeOn(Schedulers.io())

    fun transfer(params: TransferParams): Single<String> = Single.create(
        SingleOnSubscribe<String> { emt ->
            Bitmark.transfer(params, object : Callback1<String> {
                override fun onSuccess(data: String?) {
                    if (data == null) emt.onError(UnexpectedException("response is null"))
                    else emt.onSuccess(data)
                }

                override fun onError(throwable: Throwable?) {
                    emt.onError(throwable!!)
                }

            })
        }).subscribeOn(Schedulers.io())
}