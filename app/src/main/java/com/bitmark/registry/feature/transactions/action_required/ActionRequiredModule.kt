package com.bitmark.registry.feature.transactions.action_required

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class ActionRequiredModule {

    @Provides
    @FragmentScope
    fun provideViewModel(
        fragment: ActionRequiredFragment,
        accountRepo: AccountRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        realtimeBus: RealtimeBus
    ) = ActionRequiredViewModel(
        fragment.lifecycle,
        accountRepo,
        rxLiveDataTransformer,
        realtimeBus
    )
}