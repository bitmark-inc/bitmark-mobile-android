/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.splash

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.feature.sync.AssetSynchronizer
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides

@Module
class SplashModule {

    @Provides
    @ActivityScope
    fun provideViewModel(
        activity: SplashActivity,
        accountRepo: AccountRepository,
        appRepo: AppRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        wsEventBus: WebSocketEventBus,
        assetSynchronizer: AssetSynchronizer
    ): SplashViewModel {
        return SplashViewModel(
            activity.lifecycle,
            accountRepo,
            appRepo,
            bitmarkRepo,
            rxLiveDataTransformer,
            wsEventBus,
            assetSynchronizer
        )
    }

    @Provides
    @ActivityScope
    fun provideNavigator(activity: SplashActivity) = Navigator(activity)

    @Provides
    @ActivityScope
    fun provideDialogController(
        activity: SplashActivity
    ): DialogController {
        return DialogController(activity)
    }
}