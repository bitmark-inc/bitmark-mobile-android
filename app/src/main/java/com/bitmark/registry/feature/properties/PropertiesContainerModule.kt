package com.bitmark.registry.feature.properties

import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-30
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class PropertiesContainerModule {

    @Provides
    @FragmentScope
    fun provideNavigator(fragment: PropertiesContainerFragment) =
        Navigator(fragment)
}