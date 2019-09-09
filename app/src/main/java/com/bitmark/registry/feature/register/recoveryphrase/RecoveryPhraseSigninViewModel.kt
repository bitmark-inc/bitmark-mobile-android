package com.bitmark.registry.feature.register.recoveryphrase

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.Completable


/**
 * @author Hieu Pham
 * @since 2019-09-05
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseSigninViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val appRepo: AppRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel(lifecycle) {

    private val checkSameAccountLiveData = CompositeLiveData<Boolean>()

    private val clearDataLiveData = CompositeLiveData<Any>()

    internal fun checkSameAccountLiveData() =
        checkSameAccountLiveData.asLiveData()

    internal fun clearDataLiveData() = clearDataLiveData.asLiveData()

    internal fun checkSameAccount(accountNumber: String) {
        checkSameAccountLiveData.add(
            rxLiveDataTransformer.single(
                accountRepo.getAccountNumber().map { currentAccountNumber ->
                    currentAccountNumber == accountNumber
                })
        )
    }

    internal fun clearData(deviceToken: String?) {
        clearDataLiveData.add(
            rxLiveDataTransformer.completable(
                clearDataStream(
                    deviceToken
                )
            )
        )
    }

    private fun clearDataStream(deviceToken: String?): Completable {
        val deleteDeviceTokenStream = if (deviceToken == null) {
            Completable.complete()
        } else {
            appRepo.deleteDeviceToken(
                deviceToken
            ).onErrorResumeNext { Completable.complete() }
        }

        return accountRepo.removeAccess()
            .andThen(
                Completable.mergeArrayDelayError(
                    deleteDeviceTokenStream,
                    appRepo.deleteQrCodeFile(),
                    appRepo.deleteDatabase(),
                    accountRepo.getAccountNumber().flatMapCompletable { accountNumber ->
                        bitmarkRepo.deleteStoredAssetFiles(
                            accountNumber
                        )
                    }
                )
            ).andThen(
                Completable.mergeArrayDelayError(
                    appRepo.deleteMemCache(),
                    appRepo.deleteSharePref()
                )
            )
    }
}