/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.transactions.history

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides

@Module
class TransactionHistoryModule {

    @Provides
    @FragmentScope
    fun provideViewModel(
        fragment: TransactionHistoryFragment,
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        realtimeBus: RealtimeBus
    ): TransactionHistoryViewModel {
        return TransactionHistoryViewModel(
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
        fragment: TransactionHistoryFragment
    ) = Navigator(fragment)

    @Provides
    @FragmentScope
    fun provideDialogController(
        fragment: TransactionHistoryFragment
    ): DialogController {
        return DialogController(fragment.activity!!)
    }
}