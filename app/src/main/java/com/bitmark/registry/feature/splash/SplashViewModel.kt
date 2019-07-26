package com.bitmark.registry.feature.splash

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
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
    private val rxLiveDataTransformer: RxLiveDataTransformer
) :
    BaseViewModel() {

    private val getExistingAccountLiveData =
        CompositeLiveData<Triple<String, Boolean, String>>()

    private val prepareDataLiveData = CompositeLiveData<Any>()

    private val cleanupAppDataLiveData = CompositeLiveData<Any>()

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
            rxLiveDataTransformer.completable(
                cleanupAppDataStream(deviceToken)
            )
        )
    }

    private fun cleanupAppDataStream(deviceToken: String?): Completable {
        return accountRepo.checkAccessRemoved().flatMapCompletable { removed ->
            if (!removed) Completable.complete()
            else {
                val deleteDeviceToken =
                    if (deviceToken == null) Completable.complete() else appRepo.deleteDeviceToken(
                        deviceToken
                    )

                deleteDeviceToken.andThen(
                    Completable.mergeArrayDelayError(
                        appRepo.deleteQrCodeFile(),
                        appRepo.deleteDatabase(),
                        appRepo.deleteCache()
                    )
                ).andThen(appRepo.deleteSharePref())
            }
        }
    }

    internal fun prepareData(
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
                Completable.mergeDelayError(
                    listOf(
                        registerJwtStream,
                        cleanupBitmarkStream
                    )
                )
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