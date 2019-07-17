package com.bitmark.registry.feature.property_detail

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
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class PropertyDetailModule {

    @Provides
    @ActivityScope
    fun provideViewModel(
        bitmarkRepo: BitmarkRepository,
        accountRepo: AccountRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ) = PropertyDetailViewModel(bitmarkRepo, accountRepo, rxLiveDataTransformer)

    @Provides
    @ActivityScope
    fun provideNavigator(
        activity: PropertyDetailActivity
    ): Navigator<PropertyDetailActivity> {
        return Navigator(activity)
    }

    @Provides
    @ActivityScope
    fun provideDialogController(
        activity: PropertyDetailActivity
    ): DialogController {
        return DialogController(activity)
    }
}