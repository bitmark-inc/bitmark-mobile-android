package com.bitmark.registry.feature.register.authentication

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class AuthenticationModule {

    @Provides
    @FragmentScope
    fun provideViewModel(
        accountRepo: AccountRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ): AuthenticationViewModel {
        return AuthenticationViewModel(accountRepo, rxLiveDataTransformer)
    }

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: AuthenticationFragment
    ): Navigator<AuthenticationFragment> {
        return Navigator(fragment)
    }

    @Provides
    @FragmentScope
    fun provideDialogController(
        fragment: AuthenticationFragment
    ): DialogController {
        return DialogController(fragment.activity!!)
    }
}