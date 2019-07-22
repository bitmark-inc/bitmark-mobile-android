package com.bitmark.registry.feature.register.recoveryphrase

import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class RecoveryPhraseSigninModule {

    @Provides
    @ActivityScope
    fun provideNavigator(
        activity: RecoveryPhraseSigninActivity
    ): Navigator<RecoveryPhraseSigninActivity> {
        return Navigator(activity)
    }
}