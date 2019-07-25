package com.bitmark.registry.feature.recoveryphrase.test

import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer


/**
 * @author Hieu Pham
 * @since 2019-07-25
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseTestViewModel(
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel() {

    private val removeRecoveryActionRequiredLiveData = CompositeLiveData<Any>()

    internal fun removeRecoveryActionRequiredLiveData() =
        removeRecoveryActionRequiredLiveData.asLiveData()

    internal fun removeRecoveryActionRequired() =
        removeRecoveryActionRequiredLiveData.add(
            rxLiveDataTransformer.completable(
                accountRepo.deleteActionRequired(
                    ActionRequired.Id.RECOVERY_PHRASE
                )
            )
        )

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}