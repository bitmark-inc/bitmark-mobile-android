package com.bitmark.registry.feature.recoveryphrase.test

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-25
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class RecoveryPhraseTestModule {

    @Provides
    @FragmentScope
    fun provideNavigator(fragment: RecoveryPhraseTestFragment): Navigator {
        // link to AccountContainerFragment
        return Navigator(fragment.parentFragment!!)
    }

    @Provides
    @FragmentScope
    fun provideViewModel(
        fragment: RecoveryPhraseTestFragment,
        accountRepo: AccountRepository,
        appRepo: AppRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        wsEventBus: WebSocketEventBus
    ) = RecoveryPhraseTestViewModel(
        fragment.lifecycle,
        accountRepo,
        appRepo,
        bitmarkRepo,
        rxLiveDataTransformer,
        wsEventBus
    )

    @Provides
    @FragmentScope
    fun provideDialogController(fragment: RecoveryPhraseTestFragment) =
        DialogController(fragment.activity!!)
}