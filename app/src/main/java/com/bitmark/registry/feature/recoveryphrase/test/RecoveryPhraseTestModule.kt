/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.recoveryphrase.test

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.feature.sync.AssetSynchronizer
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides

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
        wsEventBus: WebSocketEventBus,
        assetSynchronizer: AssetSynchronizer
    ) = RecoveryPhraseTestViewModel(
        fragment.lifecycle,
        accountRepo,
        appRepo,
        bitmarkRepo,
        rxLiveDataTransformer,
        wsEventBus,
        assetSynchronizer
    )

    @Provides
    @FragmentScope
    fun provideDialogController(fragment: RecoveryPhraseTestFragment) =
        DialogController(fragment.activity!!)
}