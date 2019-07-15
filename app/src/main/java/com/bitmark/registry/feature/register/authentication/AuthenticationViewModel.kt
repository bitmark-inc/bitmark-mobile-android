package com.bitmark.registry.feature.register.authentication

import androidx.lifecycle.MutableLiveData
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Completable
import io.reactivex.Single


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

    internal val progressLiveData = MutableLiveData<Int>()

    internal fun registerAccount(
        timestamp: String,
        jwtSig: String,
        encPubKeySig: String? = null,
        encPubKeyHex: String? = null,
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

    internal fun registerAccountLiveData() =
        registerAccountLiveData.asLiveData()

    private fun registerAccountStream(
        timestamp: String,
        jwtSig: String,
        encPubKeySig: String? = null,
        encPubKeyHex: String? = null,
        requester: String,
        authRequired: Boolean
    ): Single<Pair<String, Boolean>> {
        val streamCount =
            if (null != encPubKeyHex && null != encPubKeySig) 3 else 2
        var progress = 0

        val registerMobileServerAccStream =
            accountRepo.registerMobileServerAccount(
                timestamp,
                jwtSig,
                requester
            ).doOnComplete {
                progressLiveData.set(++progress * 100 / streamCount)
            }

        val registerEncKeyStream =
            if (null != encPubKeyHex && null != encPubKeySig) accountRepo.registerEncPubKey(
                requester,
                encPubKeyHex,
                encPubKeySig
            ).doOnComplete {
                progressLiveData.set(++progress * 100 / streamCount)
            } else Completable.complete()

        return Completable.merge(
            listOf(registerMobileServerAccStream, registerEncKeyStream)
        ).andThen(
            accountRepo.saveAccountInfo(
                requester,
                authRequired
            ).doOnSuccess {
                progressLiveData.set(++progress * 100 / streamCount)
            }
        )
    }

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }

}