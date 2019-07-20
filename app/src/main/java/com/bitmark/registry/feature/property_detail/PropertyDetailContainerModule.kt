package com.bitmark.registry.feature.property_detail

import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-20
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class PropertyDetailContainerModule {

    @Provides
    @ActivityScope
    fun provideNavigator(activity: PropertyDetailContainerActivity) =
        Navigator(activity)
}