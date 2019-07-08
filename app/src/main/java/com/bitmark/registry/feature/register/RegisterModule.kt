package com.bitmark.registry.feature.register

import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class RegisterModule {

    @Provides
    @ActivityScope
    fun provideNavigator(
        activity: RegisterActivity
    ): Navigator<RegisterActivity> {
        return Navigator(activity)
    }

}