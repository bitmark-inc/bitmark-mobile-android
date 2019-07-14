package com.bitmark.registry.data.source.remote

import com.bitmark.apiservice.params.query.BitmarkQueryBuilder
import com.bitmark.apiservice.response.GetBitmarksResponse
import com.bitmark.apiservice.utils.callback.Callback1
import com.bitmark.apiservice.utils.record.AssetRecord
import com.bitmark.apiservice.utils.record.BitmarkRecord
import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.service.CoreApi
import com.bitmark.registry.data.source.remote.api.service.FileCourierServerApi
import com.bitmark.registry.data.source.remote.api.service.MobileServerApi
import com.bitmark.sdk.features.Bitmark
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
        at: Long = 0,
        to: String = "earlier",
        limit: Int = 100,
        pending: Boolean = false,
        issuer: String? = null,
        refAssetId: String? = null,
        loadAsset: Boolean = false
    ): Single<Pair<List<BitmarkRecord>, List<AssetRecord>>> {
        return Single.create(SingleOnSubscribe<Pair<List<BitmarkRecord>, List<AssetRecord>>> { emitter ->
            val builder =
                BitmarkQueryBuilder().limit(limit).pending(pending)
                    .loadAsset(loadAsset)
            if (at > 0) builder.at(at).to(to)
            if (null != issuer) builder.issuedBy(issuer)
            if (null != owner) builder.ownedBy(owner)
            if (null != refAssetId) builder.referencedAsset(refAssetId)
            Bitmark.list(builder, object : Callback1<GetBitmarksResponse> {
                override fun onSuccess(data: GetBitmarksResponse) {
                    emitter.onSuccess(Pair(data.bitmarks, data.assets))
                }

                override fun onError(throwable: Throwable) {
                    emitter.onError(throwable)
                }

            })
        }).subscribeOn(Schedulers.io())
    }
}