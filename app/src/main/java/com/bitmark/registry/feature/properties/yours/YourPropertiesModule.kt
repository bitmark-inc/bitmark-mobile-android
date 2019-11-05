/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.properties.yours

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides

@Module
class YourPropertiesModule {

    @Provides
    @FragmentScope
    fun provideViewModel(
        fragment: YourPropertiesFragment,
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        realtimeBus: RealtimeBus
    ): YourPropertiesViewModel {
        return YourPropertiesViewModel(
            fragment.lifecycle,
            accountRepo,
            bitmarkRepo,
            rxLiveDataTransformer,
            realtimeBus
        )
    }

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: YourPropertiesFragment
    ) = Navigator(fragment.parentFragment?.parentFragment!!)

    @Provides
    @FragmentScope
    fun provideDialogController(
        fragment: YourPropertiesFragment
    ): DialogController {
        return DialogController(fragment.activity!!)
    }
}