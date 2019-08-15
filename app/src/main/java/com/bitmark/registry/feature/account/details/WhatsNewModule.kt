package com.bitmark.registry.feature.account.details

import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-29
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class WhatsNewModule {

    @Provides
    @ActivityScope
    fun provideNavigator(activity: WhatsNewActivity) = Navigator(activity)
}