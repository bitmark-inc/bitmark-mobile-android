package com.bitmark.registry.feature.recoveryphrase.show

import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-24
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class RecoveryPhraseShowingModule {

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: RecoveryPhraseShowingFragment
    ) = Navigator(fragment.parentFragment!!)
}