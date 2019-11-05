/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.register

import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides

@Module
class RegisterModule {

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: RegisterFragment
    ) = Navigator(fragment)

}