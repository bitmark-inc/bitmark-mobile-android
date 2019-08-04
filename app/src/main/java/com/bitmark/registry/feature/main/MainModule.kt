package com.bitmark.registry.feature.main

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-12
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class MainModule {

    @Provides
    @ActivityScope
    fun provideViewModel(
        activity: MainActivity,
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository,
        wsEventBus: WebSocketEventBus,
        realtimeBus: RealtimeBus
    ) =
        MainViewModel(
            activity.lifecycle,
            accountRepo,
            bitmarkRepo,
            wsEventBus,
            realtimeBus
        )
}