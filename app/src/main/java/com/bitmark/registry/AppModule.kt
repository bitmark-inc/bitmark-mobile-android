package com.bitmark.registry

import android.app.Application
import android.content.Context
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.authentication.BmServerAuthentication
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.feature.sync.PropertySynchronizer
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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
        accountRepo: AccountRepository
    ) = BmServerAuthentication(context, appLifecycleHandler, accountRepo)

    @Provides
    @Singleton
    fun provideSynchronizer(
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository
    ) = PropertySynchronizer(bitmarkRepo, accountRepo)
}