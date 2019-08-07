package com.bitmark.registry.feature.partner_authorization

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-08-07
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class PartnerAuthorizationModule {

    @Provides
    @ActivityScope
    fun provideViewModel(
        activity: PartnerAuthorizationActivity,
        accountRepo: AccountRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ) =
        PartnerAuthorizationViewModel(
            activity.lifecycle,
            accountRepo,
            rxLiveDataTransformer
        )

    @Provides
    @ActivityScope
    fun provideNavigator(activity: PartnerAuthorizationActivity) =
        Navigator(activity)

    @Provides
    @ActivityScope
    fun provideDialogController(activity: PartnerAuthorizationActivity) =
        DialogController(activity)
}