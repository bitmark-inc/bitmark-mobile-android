/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.account

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides

@Module
class SettingsModule {

    @Provides
    @FragmentScope
    fun provideViewModel(
        fragment: SettingsFragment,
        accountRepo: AccountRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        realtimeBus: RealtimeBus
    ) = SettingsViewModel(
        fragment.lifecycle,
        accountRepo,
        rxLiveDataTransformer,
        realtimeBus
    )

    @Provides
    @FragmentScope
    fun provideNavigator(fragment: SettingsFragment): Navigator {
        // link to AccountContainerFragment
        return Navigator(fragment.parentFragment!!)
    }

}