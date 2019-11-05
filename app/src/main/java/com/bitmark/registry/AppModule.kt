/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry

import android.app.Application
import android.content.Context
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.authentication.BmServerAuthentication
import com.bitmark.registry.feature.connectivity.ConnectivityHandler
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.logging.EventLogger
import com.bitmark.registry.logging.SentryEventLogger
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context = application

    @Provides
    @Singleton
    fun provideRealtimeBus(
        bitmarkRepo: BitmarkRepository,
        accountRepo: AccountRepository
    ): RealtimeBus =
        RealtimeBus(bitmarkRepo, accountRepo)

    @Provides
    @Singleton
    fun provideWebSocketEventBus(
        accountRepo: AccountRepository,
        appLifecycleHandler: AppLifecycleHandler
    ) =
        WebSocketEventBus(accountRepo, appLifecycleHandler)

    @Provides
    @Singleton
    fun provideAppLifecycleHandler() = AppLifecycleHandler()

    @Provides
    @Singleton
    fun provideBmServerAuthentication(
        context: Context,
        appLifecycleHandler: AppLifecycleHandler,
        connectivityHandler: ConnectivityHandler,
        accountRepo: AccountRepository,
        eventLogger: EventLogger
    ) = BmServerAuthentication(
        context,
        appLifecycleHandler,
        connectivityHandler,
        accountRepo,
        eventLogger
    )

    @Provides
    @Singleton
    fun provideConnectivityHandler(context: Context) =
        ConnectivityHandler(context)

    @Provides
    @Singleton
    fun provideEventLogger(accountRepo: AccountRepository): EventLogger =
        SentryEventLogger(accountRepo)

}