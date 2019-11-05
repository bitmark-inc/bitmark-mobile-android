/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.cloud_service_sign_in

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.google_drive.GoogleDriveSignIn
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides

@Module
class CloudServiceSignInModule {

    @Provides
    @ActivityScope
    fun provideViewModel(
        activity: CloudServiceSignInActivity,
        accountRepo: AccountRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ) = CloudServiceSignInViewModel(
        activity.lifecycle,
        accountRepo,
        rxLiveDataTransformer
    )

    @Provides
    @ActivityScope
    fun provideNavigator(activity: CloudServiceSignInActivity) =
        Navigator(activity)

    @Provides
    @ActivityScope
    fun provideGoogleDriveSignIn(activity: CloudServiceSignInActivity) =
        GoogleDriveSignIn(activity)

    @Provides
    @ActivityScope
    fun provideDialogController(activity: CloudServiceSignInActivity) =
        DialogController(activity)
}