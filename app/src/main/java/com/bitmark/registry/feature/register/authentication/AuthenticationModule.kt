/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.register.authentication

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides

@Module
class AuthenticationModule {

    @Provides
    @FragmentScope
    fun provideViewModel(
        fragment: AuthenticationFragment,
        accountRepo: AccountRepository,
        appRepo: AppRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        wsEventBus: WebSocketEventBus
    ): AuthenticationViewModel {
        return AuthenticationViewModel(
            fragment.lifecycle,
            accountRepo,
            appRepo,
            rxLiveDataTransformer,
            wsEventBus
        )
    }

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: AuthenticationFragment
    ) = Navigator(fragment)

    @Provides
    @FragmentScope
    fun provideDialogController(
        fragment: AuthenticationFragment
    ): DialogController {
        return DialogController(fragment.activity!!)
    }
}