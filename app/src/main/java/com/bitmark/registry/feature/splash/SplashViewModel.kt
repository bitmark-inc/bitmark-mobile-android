package com.bitmark.registry.feature.splash

import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction


/**
 * @author Hieu Pham
 * @since 7/3/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SplashViewModel(
    private val accountRepo: AccountRepository,
    private val appRepo: AppRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val wsEventBus: WebSocketEventBus
) :
    BaseViewModel() {

    private val getExistingAccountLiveData =
        CompositeLiveData<Triple<String, Boolean, String>>()

    private val prepareDataLiveData = CompositeLiveData<Any>()

    private val cleanupAppDataLiveData = CompositeLiveData<Boolean>()

    internal fun getExistingAccount() {
        getExistingAccountLiveData.add(
            rxLiveDataTransformer.single(
                Single.zip(
                    accountRepo.getAccountInfo(),
                    accountRepo.getKeyAlias(),
                    BiFunction { a, k ->
                        Triple(a.first, a.second, k)
                    })
            )
        )
    }

    // clean up previous undone remove access action
    // this fun will be try to clean up previous account data
    internal fun cleanupAppData(deviceToken: String?) {
        cleanupAppDataLiveData.add(
            rxLiveDataTransformer.single(
                cleanupAppDataStream(deviceToken)
            )
        )
    }

    private fun cleanupAppDataStream(deviceToken: String?): Single<Boolean> {
        return accountRepo.checkAccessRemoved().flatMap { removed ->
            if (!removed) Single.just(false)
            else {
                val deleteDeviceTokenStream =
                    if (deviceToken == null) Completable.complete() else appRepo.deleteDeviceToken(
                        deviceToken
                    ).onErrorResumeNext {
                        Completable.complete() // ignore error since it's not important
                    }

                val deleteDataStream = Completable.mergeArrayDelayError(
                    appRepo.deleteQrCodeFile(),
                    appRepo.deleteDatabase(),
                    appRepo.deleteCache()
                ).andThen(appRepo.deleteSharePref())

                Completable.mergeArrayDelayError(
                    deleteDeviceTokenStream,
                    deleteDataStream
                ).andThen(Single.just(true))

            }
        }
    }

    internal fun prepareData(
        keyPair: KeyPair,
        timestamp: String,
        signature: String,
        requester: String
    ) {
        val registerJwtStream = accountRepo.registerMobileServerJwt(
            timestamp,
            signature,
            requester
        ).ignoreElement()

        val cleanupBitmarkStream =
            accountRepo.getAccountInfo().map { p -> p.first }
                .flatMapCompletable { accountNumber ->
                    bitmarkRepo.cleanupBitmark(
                        accountNumber
                    )
                }.onErrorResumeNext { Completable.complete() }


        prepareDataLiveData.add(
            rxLiveDataTransformer.completable(
                Completable.mergeArrayDelayError(
                    registerJwtStream,
                    cleanupBitmarkStream
                ).doOnComplete {
                    // connect no matter it's successful or failed
                    wsEventBus.connect(keyPair)
                }
            )
        )
    }

    internal fun getExistingAccountLiveData() =
        getExistingAccountLiveData.asLiveData()

    internal fun prepareDataLiveData() = prepareDataLiveData.asLiveData()

    internal fun cleanupAppDataLiveData() = cleanupAppDataLiveData.asLiveData()

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}