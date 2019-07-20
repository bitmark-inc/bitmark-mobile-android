package com.bitmark.registry.feature.transfer

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-20
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class TransferModule {
    @Provides
    @FragmentScope
    fun provideViewModel(
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ): TransferViewModel {
        return TransferViewModel(
            accountRepo,
            bitmarkRepo,
            rxLiveDataTransformer
        )
    }

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: TransferFragment
    ): Navigator<TransferFragment> {
        return Navigator(fragment)
    }

    @Provides
    @FragmentScope
    fun provideDialogController(
        fragment: TransferFragment
    ): DialogController {
        return DialogController(fragment.activity!!)
    }
}