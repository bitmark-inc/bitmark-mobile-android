/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api

import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.source.remote.api.middleware.*
import com.bitmark.registry.data.source.remote.api.service.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import io.reactivex.subjects.PublishSubject
import javax.inject.Singleton

@Module
class NetworkModule {

    @Singleton
    @Provides
    fun provideGson(): Gson {
        return GsonBuilder().excludeFieldsWithoutExposeAnnotation().setLenient()
            .create()
    }

    @Singleton
    @Provides
    fun provideCoreApi(
        gson: Gson, interceptor: CoreApiInterceptor
    ): CoreApi {
        return ServiceGenerator.createService(
            BuildConfig.CORE_API_ENDPOINT,
            CoreApi::class.java,
            gson,
            appInterceptors = listOf(interceptor)
        )
    }

    @Singleton
    @Provides
    fun provideMobileServerApi(
        gson: Gson,
        authInterceptor: MobileServerApiInterceptor
    ): MobileServerApi {
        return ServiceGenerator.createService(
            BuildConfig.MOBILE_SERVER_ENDPOINT,
            MobileServerApi::class.java,
            gson,
            appInterceptors = listOf(authInterceptor)
        )
    }

    @Singleton
    @Provides
    fun provideFileCourierApi(
        gson: Gson,
        authInterceptor: FileCourierServerInterceptor,
        progressInterceptor: ProgressInterceptor
    ): FileCourierServerApi {
        return ServiceGenerator.createService(
            BuildConfig.FILE_COURIER_SERVER_ENPOINT,
            FileCourierServerApi::class.java,
            gson,
            appInterceptors = listOf(progressInterceptor, authInterceptor)
        )
    }

    @Singleton
    @Provides
    fun provideKeyAccountServerApi(
        gson: Gson,
        interceptor: KeyAccountApiInterceptor
    ): KeyAccountServerApi {
        return ServiceGenerator.createService(
            BuildConfig.KEY_ACCOUNT_SERVER_ENDPOINT,
            KeyAccountServerApi::class.java,
            gson,
            appInterceptors = listOf(interceptor)
        )
    }

    @Singleton
    @Provides
    fun provideRegistryApi(
        gson: Gson,
        interceptor: RegistryApiInterceptor
    ): RegistryApi {
        return ServiceGenerator.createService(
            BuildConfig.REGISTRY_API_ENDPOINT,
            RegistryApi::class.java,
            gson,
            appInterceptors = listOf(interceptor)
        )
    }

    @Singleton
    @Provides
    fun provideProgressPublisher() = PublishSubject.create<Progress>()
}