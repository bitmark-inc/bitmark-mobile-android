package com.bitmark.registry.feature.transfer

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
 * @since 2019-07-20
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class TransferModule {
    @Provides
    @ActivityScope
    fun provideViewModel(
        activity: TransferActivity,
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        realtimeBus: RealtimeBus
    ): TransferViewModel {
        return TransferViewModel(
            activity.lifecycle,
            accountRepo,
            bitmarkRepo,
            rxLiveDataTransformer,
            realtimeBus
        )
    }

    @Provides
    @ActivityScope
    fun provideNavigator(
        activity: TransferActivity
    ) = Navigator(activity)

    @Provides
    @ActivityScope
    fun provideDialogController(
        activity: TransferActivity
    ): DialogController {
        return DialogController(activity)
    }
}