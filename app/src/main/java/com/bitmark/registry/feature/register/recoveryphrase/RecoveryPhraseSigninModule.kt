package com.bitmark.registry.feature.register.recoveryphrase

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.sync.AssetSynchronizer
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class RecoveryPhraseSigninModule {

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: RecoveryPhraseSigninFragment
    ) = Navigator(fragment)

    @Provides
    @FragmentScope
    fun provideViewModel(
        fragment: RecoveryPhraseSigninFragment,
        accountRepo: AccountRepository,
        appRepo: AppRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ) = RecoveryPhraseSigninViewModel(
        fragment.lifecycle,
        accountRepo,
        appRepo,
        bitmarkRepo,
        rxLiveDataTransformer
    )

    @Provides
    @FragmentScope
    fun provideDialogController(fragment: RecoveryPhraseSigninFragment) =
        DialogController(fragment.activity!!)
}