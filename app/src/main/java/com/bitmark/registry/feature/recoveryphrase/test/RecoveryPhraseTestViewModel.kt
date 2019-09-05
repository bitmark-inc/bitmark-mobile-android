package com.bitmark.registry.feature.recoveryphrase.test

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.feature.sync.AssetSynchronizer
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
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val appRepo: AppRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val wsEventBus: WebSocketEventBus,
    private val assetSynchronizer: AssetSynchronizer
) : BaseViewModel(lifecycle) {

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

    private fun removeAccessStream(deviceToken: String?): Completable {

        val disconnectWsStream = Completable.create { emt ->
            wsEventBus.disconnect { emt.onComplete() }
        }

        val deleteDeviceTokenStream = if (deviceToken == null) {
            Completable.complete()
        } else {
            appRepo.deleteDeviceToken(
                deviceToken
            )
        }

        val backupAssetFilesStream = accountRepo.getActionRequired()
            .map { actions -> actions.indexOfFirst { a -> a.id == ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION } == -1 }
            .flatMapCompletable { authorized ->
                if (authorized) {
                    assetSynchronizer.upload().onErrorResumeNext { e ->
                        if (e is IllegalStateException) {
                            // cloud service is not ready, maybe the session is expired
                            // rare case so temporarily ignore
                            Completable.complete()
                        } else {
                            Completable.error(e)
                        }
                    }
                } else {
                    // has not authorized cloud service, ignore the upload
                    Completable.complete()
                }
            }

        return accountRepo.removeAccess().andThen(backupAssetFilesStream)
            .andThen(
                Completable.mergeArrayDelayError(
                    deleteDeviceTokenStream,
                    appRepo.deleteQrCodeFile(),
                    appRepo.deleteDatabase(),
                    accountRepo.getAccountNumber().flatMapCompletable { accountNumber ->
                        bitmarkRepo.deleteStoredAssetFiles(
                            accountNumber
                        )
                    },
                    disconnectWsStream
                )
            ).andThen(
                Completable.mergeArrayDelayError(
                    appRepo.deleteMemCache(),
                    appRepo.deleteSharePref()
                )
            )
    }


    internal fun getAccountInfo() = getAccountInfoLiveData.add(
        rxLiveDataTransformer.single(
            Single.zip(
                accountRepo.getAccountNumber(),
                accountRepo.getKeyAlias(),
                BiFunction { a, k -> Pair(a, k) })
        )
    )

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}