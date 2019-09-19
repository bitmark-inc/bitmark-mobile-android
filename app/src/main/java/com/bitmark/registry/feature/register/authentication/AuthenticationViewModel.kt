package com.bitmark.registry.feature.register.authentication

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.cryptography.crypto.Sha3256
import com.bitmark.cryptography.crypto.encoder.Raw.RAW
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.data.model.entity.ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION
import com.bitmark.registry.data.model.entity.ActionRequired.Id.RECOVERY_PHRASE
import com.bitmark.registry.data.model.entity.ActionRequired.Type.SECURITY_ALERT
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.WebSocketEventBus
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
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val appRepo: AppRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val wsEventBus: WebSocketEventBus
) : BaseViewModel(lifecycle) {

    private val registerAccountLiveData = CompositeLiveData<Any>()

    private val updateAccountLiveData = CompositeLiveData<Any>()

    internal val progressLiveData = MutableLiveData<Int>()

    internal fun registerAccount(
        timestamp: String,
        mobileServerSig: String,
        encPubKeySig: String? = null,
        encPubKeyHex: String? = null,
        requester: String,
        authRequired: Boolean,
        keyAlias: String,
        deviceToken: String?,
        keyPair: KeyPair
    ) {
        registerAccountLiveData.add(
            rxLiveDataTransformer.completable(
                registerAccountStream(
                    timestamp,
                    mobileServerSig,
                    encPubKeySig,
                    encPubKeyHex,
                    requester,
                    authRequired,
                    keyAlias,
                    deviceToken,
                    keyPair
                )
            )
        )
    }

    internal fun updateAccount(
        keyPair: KeyPair,
        requester: String,
        authRequired: Boolean,
        keyAlias: String
    ) {
        updateAccountLiveData.add(
            rxLiveDataTransformer.completable(
                accountRepo.saveAccountInfo(
                    requester,
                    authRequired,
                    keyAlias
                ).doOnComplete {
                    wsEventBus.connect(keyPair)
                }
            )
        )
    }

    internal fun updateAccountLiveData() = updateAccountLiveData.asLiveData()

    internal fun registerAccountLiveData() =
        registerAccountLiveData.asLiveData()

    private fun registerAccountStream(
        timestamp: String,
        mobileServerSig: String,
        encPubKeySig: String? = null,
        encPubKeyHex: String? = null,
        requester: String,
        authRequired: Boolean,
        keyAlias: String,
        deviceToken: String?,
        keyPair: KeyPair
    ): Completable {
        val streamCount =
            if (null != encPubKeyHex && null != encPubKeySig) 4 else 3
        var progress = 0

        val registerMobileServerAccStream =
            accountRepo.registerMobileServerAccount(
                timestamp,
                mobileServerSig,
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

        val intercomId =
            "Registry_android_%s".format(Sha3256.hash(RAW.decode(requester)))
        val registerNotifStream =
            accountRepo.registerIntercomUser(intercomId).andThen(
                if (deviceToken == null) Completable.complete()
                else appRepo.registerDeviceToken(
                    requester,
                    timestamp,
                    mobileServerSig,
                    deviceToken,
                    intercomId
                )
            ).doOnComplete {
                progressLiveData.set(++progress * 100 / streamCount)
            }

        val saveAccountStream = Completable.mergeArrayDelayError(
            accountRepo.saveAccountInfo(
                requester,
                authRequired,
                keyAlias
            ), accountRepo.addActionRequired(buildActionRequired())
        ).doOnComplete {
            progressLiveData.set(++progress * 100 / streamCount)
        }

        return Completable.mergeArray(
            registerMobileServerAccStream,
            registerEncKeyStream
        ).andThen(
            Completable.mergeArray(
                saveAccountStream,
                registerNotifStream
            )
        ).doOnComplete {
            wsEventBus.connect(keyPair)
        }
    }

    private fun buildActionRequired() = listOf(
        ActionRequired(
            RECOVERY_PHRASE,
            SECURITY_ALERT,
            "write_down_your_recovery_phrase",
            "protect_your_bitmark_account",
            DateTimeUtil.dateToString(Date())
        ),
        ActionRequired(
            CLOUD_SERVICE_AUTHORIZATION,
            SECURITY_ALERT,
            date = DateTimeUtil.dateToString(Date())
        )
    )

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }

}