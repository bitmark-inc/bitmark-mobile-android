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


/**
 * @author Hieu Pham
 * @since 2019-07-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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