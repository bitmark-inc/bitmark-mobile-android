package com.bitmark.registry.feature.register.authentication

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Completable


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AuthenticationViewModel(
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel() {

    private val registerAccountLiveData =
        CompositeLiveData<Pair<String, Boolean>>()

    fun registerAccount(
        timestamp: String,
        jwtSig: String,
        encPubKeySig: String,
        encPubKeyHex: String,
        requester: String,
        authRequired: Boolean
    ) {
        registerAccountLiveData.add(
            rxLiveDataTransformer.single(
                registerAccountStream(
                    timestamp,
                    jwtSig,
                    encPubKeySig,
                    encPubKeyHex,
                    requester,
                    authRequired
                )
            )
        )
    }

    fun registerAccountLiveData() = registerAccountLiveData.asLiveData()

    private fun registerAccountStream(
        timestamp: String,
        jwtSig: String,
        encPubKeySig: String,
        encPubKeyHex: String,
        requester: String,
        authRequired: Boolean
    ) = Completable.merge(
        listOf(
            accountRepo.registerMobileServerAccount(
                timestamp,
                jwtSig,
                requester
            ),
            accountRepo.registerEncPubKey(requester, encPubKeyHex, encPubKeySig)
        )
    ).andThen(
        accountRepo.saveAccountInfo(requester, authRequired)
    )
}