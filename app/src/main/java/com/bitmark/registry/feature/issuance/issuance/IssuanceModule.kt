package com.bitmark.registry.feature.issuance.issuance

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-31
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class IssuanceModule {

    @Provides
    @ActivityScope
    fun provideViewModel(
        activity: IssuanceActivity,
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ) = IssuanceViewModel(
        activity.lifecycle,
        accountRepo,
        bitmarkRepo,
        rxLiveDataTransformer
    )

    @Provides
    @ActivityScope
    fun provideNavigator(activity: IssuanceActivity) = Navigator(activity)

    @Provides
    @ActivityScope
    fun provideDialogController(activity: IssuanceActivity) =
        DialogController(activity)
}