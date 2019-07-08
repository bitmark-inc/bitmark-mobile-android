package com.bitmark.registry.feature.splash

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer


/**
 * @author Hieu Pham
 * @since 7/3/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SplashViewModel(
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) :
    BaseViewModel() {

    private val getExistingAccountLiveData =
        CompositeLiveData<Pair<String, Boolean>>()
    private val registerJwtLiveData = CompositeLiveData<String>()

    fun getExistingAccount() {
        getExistingAccountLiveData.add(
            rxLiveDataTransformer.single(
                accountRepo.getAccountInfo()
            )
        )
    }

    fun registerJwt(timestamp: String, signature: String, requester: String) {
        registerJwtLiveData.add(
            rxLiveDataTransformer.single(
                accountRepo.registerMobileServerJwt(
                    timestamp,
                    signature,
                    requester
                )
            )
        )
    }

    fun getExistingAccountLiveData() =
        getExistingAccountLiveData.asLiveData()

    fun registerJwtLiveData() = registerJwtLiveData.asLiveData()

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }


}