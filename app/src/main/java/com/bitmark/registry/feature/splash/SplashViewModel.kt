package com.bitmark.registry.feature.splash

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Completable


/**
 * @author Hieu Pham
 * @since 7/3/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SplashViewModel(
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) :
    BaseViewModel() {

    private val getExistingAccountLiveData =
        CompositeLiveData<Pair<String, Boolean>>()

    private val prepareDataLiveData = CompositeLiveData<Any>()

    internal fun getExistingAccount() {
        getExistingAccountLiveData.add(
            rxLiveDataTransformer.single(
                accountRepo.getAccountInfo()
            )
        )
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

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}