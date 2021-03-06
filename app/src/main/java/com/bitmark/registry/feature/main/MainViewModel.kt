/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.main

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.apiservice.utils.error.HttpException
import com.bitmark.cryptography.crypto.Ed25519
import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.bitmark.cryptography.crypto.encoder.Raw.RAW
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.data.source.remote.api.service.ServiceGenerator
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.authentication.BmServerAuthentication
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.feature.realtime.WebSocketEventHandler
import com.bitmark.registry.feature.sync.AssetSynchronizer
import com.bitmark.registry.feature.sync.NewestPropertySynchronizer
import com.bitmark.registry.feature.sync.PropertySynchronizer
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.extension.toJson
import com.bitmark.registry.util.livedata.BufferedLiveData
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.BitmarkModelView
import io.reactivex.Completable
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

class MainViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val wsEventBus: WebSocketEventBus,
    private val realtimeBus: RealtimeBus,
    private val bmServerAuthentication: BmServerAuthentication,
    private val propertySynchronizer: PropertySynchronizer,
    private val assetSynchronizer: AssetSynchronizer,
    private val wsEventHandler: WebSocketEventHandler,
    private val newestPropertySynchronizer: NewestPropertySynchronizer
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

    internal val quotaAlmostExceededLiveData = MutableLiveData<Any>()

    internal val quotaExceededLiveData = MutableLiveData<Any>()

    internal fun getBitmarkLiveData() = getBitmarkLiveData.asLiveData()

    internal fun prepareDeepLinkHandlingLiveData() =
        prepareDeepLinkHandlingLiveData.asLiveData()

    internal fun authorizeLiveData() = authorizeLiveData.asLiveData()

    internal fun getBitmark(bitmarkId: String) {
        getBitmarkLiveData.add(
            rxLiveDataTransformer.single(
                Single.zip(
                    accountRepo.getAccountNumber(),
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
                    accountRepo.getAccountNumber(),
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

    internal fun cleanupBitmark() =
        subscribe(accountRepo.getAccountNumber().flatMapCompletable { accountNumber ->
            Completable.mergeArrayDelayError(
                bitmarkRepo.syncUpBitmark(accountNumber),
                bitmarkRepo.cleanupBitmark(accountNumber)
            )
        }.subscribe({}, {}))

    override fun onCreate() {
        super.onCreate()

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

        newestPropertySynchronizer.start()

        assetSynchronizer.setTaskProcessingListener(object :
            AssetSynchronizer.TaskProcessListener {
            override fun onError(e: Throwable) {
                super.onError(e)
                assetSyncProcessingErrorLiveData.set(e)
            }
        })

        assetSynchronizer.setCloudServiceListener(object :
            AssetSynchronizer.CloudServiceListener {

            override fun onQuotaAlmostExceeded() {
                quotaAlmostExceededLiveData.set(Any())
            }

            override fun onQuotaExceeded() {
                quotaExceededLiveData.set(Any())
            }

        })

        assetSynchronizer.start()

        wsEventHandler.start()
    }

    internal fun checkUnseenBitmark() {
        subscribe(
            accountRepo.getAccountNumber().flatMap { accountNumber ->
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

    internal fun resumeSyncAsset() = assetSynchronizer.resume()

    internal fun pauseSyncAsset() = assetSynchronizer.pause()

    override fun onDestroy() {
        wsEventBus.disconnect()
        wsEventHandler.stop()
        assetSynchronizer.stop()
        assetSynchronizer.setTaskProcessingListener(null)
        newestPropertySynchronizer.stop()
        propertySynchronizer.stop()
        realtimeBus.unsubscribe(this)
        bmServerAuthentication.destroy()
        super.onDestroy()
    }
}