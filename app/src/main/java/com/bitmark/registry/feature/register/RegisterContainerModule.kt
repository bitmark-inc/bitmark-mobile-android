package com.bitmark.registry.feature.register

import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-23
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class RegisterContainerModule {

    @Provides
    @ActivityScope
    fun provideNavigator(
        activity: RegisterContainerActivity
    ) = Navigator(activity)
}