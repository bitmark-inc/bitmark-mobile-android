package com.bitmark.registry.feature.recoveryphrase.show

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-24
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class RecoveryPhraseWarningModule {

    @Provides
    @FragmentScope
    fun provideNavigator(fragment: RecoveryPhraseWarningFragment): Navigator {
        // link to AccountContainerFragment
        return Navigator(fragment.parentFragment!!)
    }


    @Provides
    @FragmentScope
    fun provideDialogController(fragment: RecoveryPhraseWarningFragment) =
        DialogController(fragment.activity!!)

    @Provides
    @FragmentScope
    fun provideViewModel(
        fragment: RecoveryPhraseWarningFragment,
        accountRepo: AccountRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ) = RecoveryPhraseWarningViewModel(
        fragment.lifecycle,
        accountRepo,
        rxLiveDataTransformer
    )
}