package com.bitmark.registry.feature.register.authentication

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.di.ActivityScope
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
    @ActivityScope
    fun provideViewModel(
        accountRepo: AccountRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ): AuthenticationViewModel {
        return AuthenticationViewModel(accountRepo, rxLiveDataTransformer)
    }

    @Provides
    @ActivityScope
    fun provideNavigator(
        activity: AuthenticationActivity
    ): Navigator<AuthenticationActivity> {
        return Navigator(activity)
    }

    @Provides
    @ActivityScope
    fun provideDialogController(
        activity: AuthenticationActivity
    ): DialogController {
        return DialogController(activity)
    }
}