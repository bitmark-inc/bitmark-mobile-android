package com.bitmark.registry.data.source.remote

import com.bitmark.apiservice.params.IssuanceParams
import com.bitmark.apiservice.params.RegistrationParams
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.apiservice.params.query.BitmarkQueryBuilder
import com.bitmark.apiservice.params.query.TransactionQueryBuilder
import com.bitmark.apiservice.response.GetBitmarkResponse
import com.bitmark.apiservice.response.GetBitmarksResponse
import com.bitmark.apiservice.response.GetTransactionsResponse
import com.bitmark.apiservice.response.RegistrationResponse
import com.bitmark.apiservice.utils.callback.Callback1
import com.bitmark.apiservice.utils.error.UnexpectedException
import com.bitmark.apiservice.utils.record.AssetRecord
import com.bitmark.registry.data.model.entity.BlockData
import com.bitmark.registry.data.model.entity.AssetDataR
import com.bitmark.registry.data.model.entity.BitmarkDataR
import com.bitmark.registry.data.model.entity.TransactionDataR
import com.bitmark.registry.data.source.Constant.OMNISCIENT_ASSET_ID
import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.error.HttpException
import com.bitmark.registry.data.source.remote.api.middleware.Progress
import com.bitmark.registry.data.source.remote.api.middleware.RxErrorHandlingComposer
import com.bitmark.registry.data.source.remote.api.request.ProgressListener
import com.bitmark.registry.data.source.remote.api.request.ProgressRequestBody
import com.bitmark.registry.data.source.remote.api.response.AssetFileInfoResponse
import com.bitmark.registry.data.source.remote.api.response.DownloadAssetFileResponse
import com.bitmark.registry.data.source.remote.api.service.*
import com.bitmark.registry.util.encryption.SessionData
import com.bitmark.sdk.features.Asset
import com.bitmark.sdk.features.Bitmark
import com.bitmark.sdk.features.Transaction
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BitmarkRemoteDataSource @Inject constructor(
    coreApi: CoreApi,
    mobileServerApi: MobileServerApi,
    fileCourierServerApi: FileCourierServerApi,
    keyAccountServerApi: KeyAccountServerApi,
    registryApi: RegistryApi,
    converter: Converter,
    rxErrorHandlingComposer: RxErrorHandlingComposer,
    private val progressPublisher: PublishSubject<Progress>
) : RemoteDataSource(
    coreApi,
    mobileServerApi,
    fileCourierServerApi,
    keyAccountServerApi,
    registryApi,
    converter,
    rxErrorHandlingComposer
) {

    //region Bitmark
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
    ): Single<Pair<List<BitmarkDataR>, List<AssetDataR>>> {
        return rxErrorHandlingComposer.single(SingleOnSubscribe<Pair<List<BitmarkDataR>, List<AssetDataR>>> { emt ->
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
                    emt.onSuccess(
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
                    emt.onError(throwable)
                }

            })
        }).subscribeOn(Schedulers.io()).flatMap { p ->
            val bitmarks = p.first
            val musicClaimingBm =
                bitmarks.filter { b -> b.assetId == OMNISCIENT_ASSET_ID }
            if (musicClaimingBm.isNullOrEmpty()) Single.just(p)
            else {
                getAssetClaimingInfo(OMNISCIENT_ASSET_ID).map { res ->
                    musicClaimingBm.forEach { b ->
                        b.totalEdition = res.limitedEdition
                        b.readableIssuer = res.issuer
                    }
                    p
                }
            }
        }
    }

    fun transfer(params: TransferParams): Single<String> =
        rxErrorHandlingComposer.single(
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

    fun getBitmark(bitmarkId: String, loadAsset: Boolean = false) =
        rxErrorHandlingComposer.single(SingleOnSubscribe<Pair<BitmarkDataR?, AssetDataR?>> { emt ->
            Bitmark.get(
                bitmarkId,
                loadAsset,
                object : Callback1<GetBitmarkResponse> {
                    override fun onSuccess(data: GetBitmarkResponse?) {
                        val bitmark =
                            if (data?.bitmark == null) null else converter.mapBitmark(
                                data.bitmark
                            )
                        val asset =
                            if (data?.asset == null) null else converter.mapAsset(
                                data.asset
                            )
                        emt.onSuccess(Pair(bitmark, asset))
                    }

                    override fun onError(throwable: Throwable?) {
                        emt.onError(throwable!!)
                    }
                })
        }).subscribeOn(Schedulers.io()).flatMap { p ->
            val bitmark = p.first
            if (OMNISCIENT_ASSET_ID == bitmark?.assetId) {
                getAssetClaimingInfo(OMNISCIENT_ASSET_ID).map { res ->
                    bitmark.totalEdition = res.limitedEdition
                    bitmark.readableIssuer = res.issuer
                    p
                }
            } else {
                Single.just(p)
            }
        }

    fun issueBitmark(params: IssuanceParams) =
        rxErrorHandlingComposer.single(SingleOnSubscribe<List<String>> { emt ->
            Bitmark.issue(params, object : Callback1<List<String>> {
                override fun onSuccess(data: List<String>?) {
                    emt.onSuccess(data!!)
                }

                override fun onError(throwable: Throwable?) {
                    emt.onError(throwable!!)
                }

            })
        }).subscribeOn(Schedulers.io())

    fun registerAsset(params: RegistrationParams) =
        rxErrorHandlingComposer.single(SingleOnSubscribe<String> { emt ->
            Asset.register(params, object : Callback1<RegistrationResponse> {
                override fun onSuccess(data: RegistrationResponse?) {
                    emt.onSuccess(data!!.assets.first().id)
                }

                override fun onError(throwable: Throwable?) {
                    emt.onError(throwable!!)
                }

            })
        }).subscribeOn(Schedulers.io())


    //endregion Bitmark


    //region Tx
    fun listTxs(
        owner: String? = null,
        assetId: String? = null,
        bitmarkId: String? = null,
        loadAsset: Boolean = false,
        blockNumber: Long? = null,
        isPending: Boolean? = false,
        sent: Boolean = false,
        at: Long = 0,
        to: String = "ealier",
        limit: Int = 100,
        loadBlock: Boolean = false
    ): Single<Triple<List<TransactionDataR>, List<AssetDataR>, List<BlockData>>> =
        rxErrorHandlingComposer.single(SingleOnSubscribe<Triple<List<TransactionDataR>, List<AssetDataR>, List<BlockData>>> { emt ->
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
            if (at > 0) queryBuilder.at(at).to(to)

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

    //endregion Tx

    //region Asset

    fun downloadAssetFile(
        assetId: String,
        sender: String,
        receiver: String,
        progress: (Int) -> Unit
    ): Single<DownloadAssetFileResponse> {

        val subscription = progressPublisher.subscribe { p ->
            if (p.identifier == assetId) {
                progress.invoke(p.progress)
            }
        }

        return fileCourierServerApi.downloadAssetFile(
            assetId,
            assetId,
            sender,
            receiver
        ).flatMap { res ->

            subscription.dispose()

            if (!res.isSuccessful) {
                val code = res.code()
                Single.error<DownloadAssetFileResponse>(
                    HttpException(code)
                )
            } else {
                val headers = res.headers()
                val algorithm = headers["data-key-alg"]
                val encDataKey = headers["enc-data-key"]
                val fileName = headers["file-name"]
                val resBody = res.body()

                Single.just(
                    DownloadAssetFileResponse(
                        SessionData(encDataKey!!, algorithm!!),
                        fileName!!,
                        resBody?.bytes()!!
                    )
                )
            }
        }.subscribeOn(Schedulers.io())
    }

    fun deleteAssetFile(
        assetId: String,
        sender: String,
        receiver: String
    ): Completable =
        fileCourierServerApi.deleteAssetFile(
            assetId,
            sender,
            receiver
        ).subscribeOn(
            Schedulers.io()
        )

    fun uploadAssetFile(
        assetId: String,
        sender: String,
        sessionData: SessionData,
        access: String,
        file: File,
        progress: (Int) -> Unit
    ): Completable {

        val fileReqBody = ProgressRequestBody(
            file.asRequestBody(),
            object : ProgressListener {
                override fun update(
                    bytesRead: Long,
                    contentLength: Long,
                    done: Boolean
                ) {
                    progress((bytesRead * 100 / contentLength).toInt())
                }

            })
        val filePart =
            MultipartBody.Part.createFormData("file", file.name, fileReqBody)
        val keyAlgorithmReqBody = sessionData.algorithm.toRequestBody()
        val encKeyReqBody = sessionData.encryptedKey.toRequestBody()
        val orgContentTypeReqBody = "*".toRequestBody()
        val accessReqBody = access.toRequestBody()
        return fileCourierServerApi.uploadAssetFile(
            assetId,
            sender,
            keyAlgorithmReqBody,
            encKeyReqBody,
            orgContentTypeReqBody,
            accessReqBody,
            filePart
        ).subscribeOn(Schedulers.io())
    }

    fun checkExistingAssetFile(
        assetId: String,
        sender: String
    ): Single<AssetFileInfoResponse> {
        return fileCourierServerApi.checkExistingAssetFile(
            assetId,
            sender
        )
            .flatMap { res ->
                if (!res.isSuccessful) {
                    val code = res.code()
                    Single.error<AssetFileInfoResponse>(
                        HttpException(code)
                    )
                } else {
                    val headers = res.headers()
                    val keyAlgorithm = headers["data-key-alg"]
                    val encKey = headers["enc-data-key"]
                    val orgContentType = headers["orig-content-type"]
                    val expiration = headers["expiration"]
                    val name = headers["file-name"]
                    val date = headers["date"]
                    Single.just(
                        AssetFileInfoResponse(
                            if (keyAlgorithm != null && encKey != null) SessionData(
                                encKey,
                                keyAlgorithm
                            ) else null,
                            orgContentType,
                            expiration,
                            name,
                            date
                        )
                    )
                }
            }.onErrorResumeNext { e ->
                if (e is HttpException && (e.code in 400 until 500)) {
                    Single.just(AssetFileInfoResponse.newInstance())
                } else Single.error<AssetFileInfoResponse>(e)
            }.subscribeOn(Schedulers.io())
    }

    fun grantAccessAssetFile(
        assetId: String,
        sender: String,
        access: String
    ): Completable {
        val accessReqBody = access.toRequestBody()
        return fileCourierServerApi.grantAccessAssetFile(
            assetId,
            sender,
            accessReqBody
        ).subscribeOn(Schedulers.io())
    }

    fun getAsset(id: String) =
        rxErrorHandlingComposer.single(SingleOnSubscribe<AssetDataR> { emt ->
            Asset.get(id, object : Callback1<AssetRecord> {
                override fun onSuccess(data: AssetRecord?) {
                    emt.onSuccess(converter.mapAsset(data!!))
                }

                override fun onError(throwable: Throwable?) {
                    emt.onError(throwable!!)
                }

            })
        }).subscribeOn(Schedulers.io())

    fun getDownloadableAssets(receiver: String) =
        fileCourierServerApi.getDownloadableAssets(receiver).map { res ->
            res["file_ids"]
        }.subscribeOn(Schedulers.io())

    //endregion Asset

    //region Claim

    fun getAssetClaimingInfo(assetId: String) =
        registryApi.getAssetClaimingInfo(assetId).subscribeOn(Schedulers.io())

    fun getAssetClaimRequests(assetId: String) =
        mobileServerApi.getAssetClaimRequests(assetId).subscribeOn(Schedulers.io())

    //endregion Claim


}