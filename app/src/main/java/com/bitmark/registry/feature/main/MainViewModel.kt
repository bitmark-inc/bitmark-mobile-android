package com.bitmark.registry.feature.main

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.apiservice.utils.error.HttpException
import com.bitmark.cryptography.crypto.Ed25519
import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.bitmark.cryptography.crypto.encoder.Raw.RAW
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.remote.api.service.ServiceGenerator
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.authentication.BmServerAuthentication
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.feature.sync.AssetSynchronizer
import com.bitmark.registry.feature.sync.PropertySynchronizer
import com.bitmark.registry.util.extension.isDbRecNotFoundError
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.extension.toJson
import com.bitmark.registry.util.livedata.BufferedLiveData
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.BitmarkModelView
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException


/**
 * @author Hieu Pham
 * @since 2019-07-27
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MainViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val wsEventBus: WebSocketEventBus,
    private val realtimeBus: RealtimeBus,
    private val bmServerAuthentication: BmServerAuthentication,
    private val propertySynchronizer: PropertySynchronizer,
    private val assetSynchronizer: AssetSynchronizer
) :
    BaseViewModel(lifecycle) {

    internal val checkBitmarkSeenLiveData = BufferedLiveData<Boolean>(lifecycle)

    internal val checkActionRequiredLiveData =
        BufferedLiveData<List<ActionRequired.Id>>(lifecycle)

    private val getBitmarkLiveData = CompositeLiveData<BitmarkModelView>()

    private val prepareDeepLinkHandlingLiveData =
        CompositeLiveData<Pair<String, String>>()

    private val authorizeLiveData = CompositeLiveData<String>()

    internal val assetSyncProcessingErrorLiveData = MutableLiveData<Throwable>()

    internal fun getBitmarkLiveData() = getBitmarkLiveData.asLiveData()

    internal fun prepareDeepLinkHandlingLiveData() =
        prepareDeepLinkHandlingLiveData.asLiveData()

    internal fun authorizeLiveData() = authorizeLiveData.asLiveData()

    internal fun getBitmark(bitmarkId: String) {
        getBitmarkLiveData.add(
            rxLiveDataTransformer.single(
                Single.zip(
                    accountRepo.getAccountInfo().map { a -> a.first },
                    bitmarkRepo.syncBitmark(
                        bitmarkId, true
                    ),
                    BiFunction { accountNumber, bitmark ->
                        BitmarkModelView.newInstance(
                            bitmark,
                            accountNumber
                        )
                    }
                )
            )
        )
    }

    internal fun prepareDeepLinkHandling() =
        prepareDeepLinkHandlingLiveData.add(
            rxLiveDataTransformer.single(
                Single.zip(
                    accountRepo.getAccountInfo().map { a -> a.first },
                    accountRepo.getKeyAlias(),
                    BiFunction { accountNumber, keyAlias ->
                        Pair(
                            accountNumber,
                            keyAlias
                        )
                    })
            )
        )

    internal fun authorize(
        accountNumber: String,
        url: String,
        code: String,
        keyPair: KeyPair
    ) {
        authorizeLiveData.add(
            rxLiveDataTransformer.single(
                authorizeStream(accountNumber, url, code, keyPair)
            )
        )
    }

    private fun authorizeStream(
        accountNumber: String,
        url: String,
        code: String,
        keyPair: KeyPair
    ) = Single.create(SingleOnSubscribe<String> { emt ->
        val message = "Verify|$code"
        val signature = HEX.encode(
            Ed25519.sign(
                RAW.decode(message),
                keyPair.privateKey().toBytes()
            )
        )
        val params = mapOf<String, String>(
            "bitmark_account" to accountNumber,
            "code" to code,
            "signature" to signature
        )
        val requestBody = params.toJson().toRequestBody()
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBody).build()

        val httpClient = ServiceGenerator.buildHttpClient()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emt.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful)
                    emt.onSuccess(url)
                else
                    emt.onError(
                        HttpException(
                            response.code,
                            response.body!!.string()
                        )
                    )
            }

        })

    }).subscribeOn(Schedulers.io())

    override fun onCreate() {
        super.onCreate()

        wsEventBus.bitmarkChangedPublisher.subscribe(this) { m ->
            val bitmarkId = m["bitmark_id"] as String
            val presence = m["presence"] as Boolean
            processBitmarkChangedEvent(bitmarkId, presence)
        }

        wsEventBus.newPendingTxPublisher.subscribe(this) { m ->
            val owner = m["owner"] ?: return@subscribe
            val previousTxId = m["prev_tx_id"] ?: return@subscribe
            processNewPendingTxEvent(owner, previousTxId)
        }

        wsEventBus.newPendingIssuancePublisher.subscribe(this) { bitmarkId ->
            processNewPendingIssuance(bitmarkId)
        }

        realtimeBus.bitmarkSeenPublisher.subscribe(this) {
            checkUnseenBitmark()
        }

        realtimeBus.bitmarkDeletedPublisher.subscribe(this) {
            checkUnseenBitmark()
        }

        realtimeBus.bitmarkSavedPublisher.subscribe(this) {
            checkUnseenBitmark()
        }

        realtimeBus.actionRequiredDeletedPublisher.subscribe(this) {
            checkActionRequired()
        }

        realtimeBus.actionRequiredAddedPublisher.subscribe(this) {
            checkActionRequired()
        }

        propertySynchronizer.start()

        assetSynchronizer.setTaskProcessingListener(object :
            AssetSynchronizer.TaskProcessListener {
            override fun onError(e: Throwable) {
                super.onError(e)
                assetSyncProcessingErrorLiveData.set(e)
            }
        })

        assetSynchronizer.start()
    }

    internal fun checkUnseenBitmark() {
        subscribe(
            accountRepo.getAccountInfo().map { a -> a.first }.flatMap { accountNumber ->
                bitmarkRepo.checkUnseenBitmark(
                    accountNumber
                )
            }
                .observeOn(
                    AndroidSchedulers.mainThread()
                ).subscribe { has, e ->
                    if (e == null) {
                        checkBitmarkSeenLiveData.setValue(has)
                    }
                })
    }

    internal fun checkActionRequired() {
        subscribe(
            accountRepo.getActionRequired().map { actions -> actions.map { a -> a.id } }.observeOn(
                AndroidSchedulers.mainThread()
            ).subscribe { ids, e ->
                if (e == null) {
                    checkActionRequiredLiveData.setValue(ids)
                }
            })
    }

    private fun processBitmarkChangedEvent(
        bitmarkId: String,
        presence: Boolean
    ) {
        val stream = getAccountNumber().flatMap { accountNumber ->
            bitmarkRepo.maxStoredRelevantTxOffset(accountNumber)
                .flatMap { offset ->
                    bitmarkRepo.syncTxs(
                        owner = accountNumber,
                        sent = true,
                        bitmarkId = bitmarkId,
                        loadAsset = true,
                        loadBlock = true,
                        at = offset,
                        to = "later"
                    )
                }
        }.flatMapMaybe {
            if (presence) bitmarkRepo.syncBitmark(
                bitmarkId,
                true
            ).toMaybe() else Maybe.empty()
        }
        subscribe(stream.subscribe({}, {}))
    }

    private fun processNewPendingTxEvent(owner: String, prevTxId: String) {

        val deleteBmStream =
            fun(accountNumber: String) =
                bitmarkRepo.getStoredTxById(prevTxId).flatMapCompletable { tx ->
                    bitmarkRepo.deleteStoredBitmark(
                        accountNumber,
                        tx.bitmarkId,
                        tx.assetId
                    )
                }


        subscribe(
            getAccountNumber().flatMapCompletable { accountNumber ->
                if (BuildConfig.ZERO_ADDRESS == owner) {
                    // outgoing tx for delete
                    deleteBmStream(accountNumber)
                } else {
                    bitmarkRepo.maxStoredRelevantTxOffset(accountNumber)
                        .flatMap { offset ->
                            bitmarkRepo.syncTxs(
                                owner = accountNumber,
                                sent = true,
                                isPending = true,
                                loadAsset = true,
                                loadBlock = true,
                                at = offset,
                                to = "later"
                            )
                        }.flatMapCompletable {
                            if (owner != accountNumber) {
                                // outgoing tx
                                deleteBmStream(accountNumber)
                            } else {
                                // incoming tx
                                bitmarkRepo.maxStoredBitmarkOffset()
                                    .flatMap { offset ->
                                        bitmarkRepo.syncBitmarks(
                                            owner = accountNumber,
                                            at = offset,
                                            to = "later",
                                            pending = true,
                                            loadAsset = true
                                        )
                                    }.ignoreElement()
                            }
                        }
                }
            }.subscribe(
                {},
                {})
        )
    }

    private fun processNewPendingIssuance(bitmarkId: String) {
        subscribe(bitmarkRepo.getStoredBitmarkById(bitmarkId).onErrorResumeNext { e ->
            if (e.isDbRecNotFoundError()) Single.zip(bitmarkRepo.syncBitmark(
                bitmarkId,
                true
            ), bitmarkRepo.syncTxs(
                bitmarkId = bitmarkId,
                isPending = true,
                loadBlock = true,
                loadAsset = true
            ), BiFunction { bitmark, _ -> bitmark })
            else Single.error(e)
        }.subscribe({}, {}))
    }

    private fun getAccountNumber() =
        accountRepo.getAccountInfo().map { p -> p.first }

    internal fun resumeSyncAsset() = assetSynchronizer.resume()

    internal fun pauseSyncAsset() = assetSynchronizer.pause()

    override fun onDestroy() {
        assetSynchronizer.stop()
        assetSynchronizer.setTaskProcessingListener(null)
        propertySynchronizer.stop()
        realtimeBus.unsubscribe(this)
        wsEventBus.disconnect()
        bmServerAuthentication.destroy()
        super.onDestroy()
    }
}