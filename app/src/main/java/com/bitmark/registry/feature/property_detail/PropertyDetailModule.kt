/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.property_detail

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides

@Module
class PropertyDetailModule {

    @Provides
    @ActivityScope
    fun provideViewModel(
        activity: PropertyDetailActivity,
        bitmarkRepo: BitmarkRepository,
        accountRepo: AccountRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        realtimeBus: RealtimeBus
    ) = PropertyDetailViewModel(
        activity.lifecycle,
        bitmarkRepo,
        accountRepo,
        rxLiveDataTransformer,
        realtimeBus
    )

    @Provides
    @ActivityScope
    fun provideNavigator(
        activity: PropertyDetailActivity
    ) = Navigator(activity)

    @Provides
    @ActivityScope
    fun provideDialogController(
        activity: PropertyDetailActivity
    ): DialogController {
        return DialogController(activity)
    }
}