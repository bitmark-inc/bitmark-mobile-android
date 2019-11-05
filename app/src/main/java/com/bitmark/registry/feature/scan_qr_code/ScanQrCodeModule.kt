/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.scan_qr_code

import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides

@Module
class ScanQrCodeModule {

    @Provides
    @ActivityScope
    fun provideNavigator(activity: ScanQrCodeActivity) = Navigator(activity)

    @Provides
    @ActivityScope
    fun provideDialogController(activity: ScanQrCodeActivity) =
        DialogController(activity)
}