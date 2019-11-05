/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.recoveryphrase.show

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides

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