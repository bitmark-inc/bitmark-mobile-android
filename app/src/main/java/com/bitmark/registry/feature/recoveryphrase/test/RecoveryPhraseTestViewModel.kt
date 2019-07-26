package com.bitmark.registry.feature.recoveryphrase.test

import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction


/**
 * @author Hieu Pham
 * @since 2019-07-25
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseTestViewModel(
    private val accountRepo: AccountRepository,
    private val appRepo: AppRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel() {

    private val removeRecoveryActionRequiredLiveData = CompositeLiveData<Any>()

    private val removeAccessLiveData = CompositeLiveData<Any>()

    private val getAccountInfoLiveData =
        CompositeLiveData<Pair<String, String>>()

    internal fun removeRecoveryActionRequiredLiveData() =
        removeRecoveryActionRequiredLiveData.asLiveData()

    internal fun removeAccessLiveData() = removeAccessLiveData.asLiveData()

    internal fun getAccountInfoLiveData() = getAccountInfoLiveData.asLiveData()

    internal fun removeRecoveryActionRequired() =
        removeRecoveryActionRequiredLiveData.add(
            rxLiveDataTransformer.completable(
                accountRepo.deleteActionRequired(
                    ActionRequired.Id.RECOVERY_PHRASE
                )
            )
        )

    internal fun removeAccess(deviceToken: String?) =
        removeAccessLiveData.add(
            rxLiveDataTransformer.completable(
                removeAccessStream(deviceToken)
            )
        )

    private fun removeAccessStream(deviceToken: String?) =
        accountRepo.removeAccess().andThen(
            if (deviceToken == null) Completable.complete() else appRepo.deleteDeviceToken(
                deviceToken
            )
        ).andThen(
            Completable.mergeArrayDelayError(
                appRepo.deleteQrCodeFile(),
                appRepo.deleteDatabase(),
                appRepo.deleteCache()
            )
        ).andThen(appRepo.deleteSharePref())

    internal fun getAccountInfo() = getAccountInfoLiveData.add(
        rxLiveDataTransformer.single(
            Single.zip(
                accountRepo.getAccountInfo(),
                accountRepo.getKeyAlias(),
                BiFunction { a, k -> Pair(a.first, k) })
        )
    )

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}