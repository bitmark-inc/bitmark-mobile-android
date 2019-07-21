package com.bitmark.registry.feature.main.scan_qr_code

import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class ScanQrCodeModule {

    @Provides
    @ActivityScope
    fun provideNavigator(activity: ScanQrCodeActivity) = Navigator(activity)
}