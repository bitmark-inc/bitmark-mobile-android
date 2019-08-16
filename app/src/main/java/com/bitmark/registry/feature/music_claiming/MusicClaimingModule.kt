package com.bitmark.registry.feature.music_claiming

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-08-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class MusicClaimingModule {

    @Provides
    @ActivityScope
    fun provideNavigator(activity: MusicClaimingActivity) = Navigator(activity)

    @Provides
    @ActivityScope
    fun provideViewModel(
        activity: MusicClaimingActivity,
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        realtimeBus: RealtimeBus
    ) = MusicClaimingViewModel(
        activity.lifecycle,
        accountRepo,
        bitmarkRepo,
        rxLiveDataTransformer,
        realtimeBus
    )

    @Provides
    @ActivityScope
    fun provideDialogController(activity: MusicClaimingActivity) =
        DialogController(activity)
}