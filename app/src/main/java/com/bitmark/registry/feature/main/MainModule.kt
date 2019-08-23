package com.bitmark.registry.feature.main

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.authentication.BmServerAuthentication
import com.bitmark.registry.feature.google_drive.GoogleDriveSignIn
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.feature.sync.AssetSynchronizer
import com.bitmark.registry.feature.sync.PropertySynchronizer
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
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
        rxLiveDataTransformer: RxLiveDataTransformer,
        wsEventBus: WebSocketEventBus,
        realtimeBus: RealtimeBus,
        bmServerAuthentication: BmServerAuthentication,
        propertySynchronizer: PropertySynchronizer,
        assetSynchronizer: AssetSynchronizer
    ) =
        MainViewModel(
            activity.lifecycle,
            accountRepo,
            bitmarkRepo,
            rxLiveDataTransformer,
            wsEventBus,
            realtimeBus,
            bmServerAuthentication,
            propertySynchronizer,
            assetSynchronizer
        )

    @Provides
    @ActivityScope
    fun provideNavigator(activity: MainActivity) = Navigator(activity)

    @Provides
    @ActivityScope
    fun provideDialogController(activity: MainActivity) =
        DialogController(activity)

    @Provides
    @ActivityScope
    fun provideGoogleDriveSignIn(activity: MainActivity) =
        GoogleDriveSignIn(activity)
}