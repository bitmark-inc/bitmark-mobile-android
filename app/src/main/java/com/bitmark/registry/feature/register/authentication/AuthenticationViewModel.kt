package com.bitmark.registry.feature.register.authentication

import androidx.lifecycle.MutableLiveData
import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.data.model.ActionRequired.Id.RECOVERY_PHRASE
import com.bitmark.registry.data.model.ActionRequired.Type.SECURITY_ALERT
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.DateTimeUtil
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Completable
import java.util.*


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

    private val registerAccountLiveData = CompositeLiveData<Any>()

    internal val progressLiveData = MutableLiveData<Int>()

    internal fun registerAccount(
        timestamp: String,
        jwtSig: String,
        encPubKeySig: String? = null,
        encPubKeyHex: String? = null,
        requester: String,
        authRequired: Boolean,
        keyAlias: String
    ) {
        registerAccountLiveData.add(
            rxLiveDataTransformer.completable(
                registerAccountStream(
                    timestamp,
                    jwtSig,
                    encPubKeySig,
                    encPubKeyHex,
                    requester,
                    authRequired,
                    keyAlias
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
        authRequired: Boolean,
        keyAlias: String
    ): Completable {
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

        val saveAccountStream = Completable.mergeArrayDelayError(
            accountRepo.saveAccountInfo(
                requester,
                authRequired,
                keyAlias
            ), accountRepo.addActionRequired(buildActionRequired())
        ).doOnComplete {
            progressLiveData.set(++progress * 100 / streamCount)
        }

        return Completable.merge(
            listOf(registerMobileServerAccStream, registerEncKeyStream)
        ).andThen(saveAccountStream)
    }

    private fun buildActionRequired() = listOf(
        ActionRequired(
            RECOVERY_PHRASE,
            SECURITY_ALERT,
            "write_down_your_recovery_phrase",
            "protect_your_bitmark_account",
            DateTimeUtil.dateToString(Date())
        )
    )

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }

}