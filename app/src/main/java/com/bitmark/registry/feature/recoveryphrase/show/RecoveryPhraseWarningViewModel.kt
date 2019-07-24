package com.bitmark.registry.feature.recoveryphrase.show

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer


/**
 * @author Hieu Pham
 * @since 2019-07-24
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseWarningViewModel(
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel() {

    private val getAccountNumberLiveData = CompositeLiveData<String>()

    internal fun getAccountNumberLiveData() =
        getAccountNumberLiveData.asLiveData()

    internal fun getAccountNumber() =
        getAccountNumberLiveData.add(rxLiveDataTransformer.single(accountRepo.getAccountInfo().map { a -> a.first }))

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }

}