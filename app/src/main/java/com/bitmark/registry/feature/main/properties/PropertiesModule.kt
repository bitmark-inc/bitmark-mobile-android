package com.bitmark.registry.feature.main.properties

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 7/8/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class PropertiesModule {

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: PropertiesFragment
    ) = Navigator(fragment)

    @Provides
    @FragmentScope
    fun provideViewModel(
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        realtimeBus: RealtimeBus
    ) = PropertiesViewModel(
        accountRepo,
        bitmarkRepo,
        rxLiveDataTransformer,
        realtimeBus
    )
}