package com.bitmark.registry.feature.splash

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.util.extension.set
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
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val appRepo: AppRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val wsEventBus: WebSocketEventBus
) :
    BaseViewModel(lifecycle) {

    private val getExistingAccountLiveData =
        CompositeLiveData<Triple<String, Boolean, String>>()

    private val prepareDataLiveData = CompositeLiveData<Any>()

    private val cleanupAppDataLiveData = CompositeLiveData<Boolean>()

    internal val progressLiveData = MutableLiveData<Int>()

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
            if (!removed) Single.just(false).doOnSuccess {
                progressLiveData.set(
                    100
                )
            }
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

                var progress = 0
                val streamCount = 2
                val completeAction =
                    { progressLiveData.set(++progress / streamCount * 100) }


                Completable.mergeArrayDelayError(
                    deleteDeviceTokenStream.doOnComplete(completeAction),
                    deleteDataStream.doOnComplete(completeAction)
                ).andThen(Single.just(true))
                    .doOnSubscribe { progressLiveData.set(0) }

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
            accountRepo.getAccountNumber()
                .flatMapCompletable { accountNumber ->
                    bitmarkRepo.cleanupBitmark(
                        accountNumber
                    ).andThen(
                        Single.mergeDelayError(
                            bitmarkRepo.syncLatestPendingBitmarks(accountNumber),
                            bitmarkRepo.syncLatestRelevantPendingTxs(accountNumber)
                        ).ignoreElements()
                    )
                }.onErrorResumeNext { Completable.complete() }

        var progress = 0
        val streamCount = 2
        val completeAction =
            { progressLiveData.set(++progress / streamCount * 100) }


        prepareDataLiveData.add(
            rxLiveDataTransformer.completable(
                Completable.mergeArrayDelayError(
                    registerJwtStream.doOnComplete(completeAction),
                    cleanupBitmarkStream.doOnComplete(completeAction)
                ).doOnSubscribe { progressLiveData.set(0) }.doOnComplete {
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