package com.bitmark.registry.data.source.remote.api

import com.bitmark.registry.data.source.remote.api.middleware.CoreApiInterceptor
import com.bitmark.registry.data.source.remote.api.middleware.FileCourierServerInterceptor
import com.bitmark.registry.data.source.remote.api.middleware.MobileServerApiInterceptor
import com.bitmark.registry.data.source.remote.api.service.CoreApi
import com.bitmark.registry.data.source.remote.api.service.FileCourierServerApi
import com.bitmark.registry.data.source.remote.api.service.MobileServerApi
import com.bitmark.registry.data.source.remote.api.service.ServiceGenerator
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
class NetworkModule {

    @Singleton
    @Provides
    fun provideGson(): Gson {
        return GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
    }

    @Singleton
    @Provides
    fun provideCoreApi(
        endpoint: String, timeout: Long, gson: Gson,
        interceptor: CoreApiInterceptor
    ): CoreApi {
        return ServiceGenerator.createService(
            endpoint, CoreApi::class.java, gson, interceptor, timeout
        )
    }

    @Singleton
    @Provides
    fun provideCoreApiInterceptor(): CoreApiInterceptor {
        return CoreApiInterceptor()
    }

    @Singleton
    @Provides
    fun provideMobileServerApi(
        endpoint: String, timeout: Long, gson: Gson,
        interceptor: MobileServerApiInterceptor
    ): MobileServerApi {
        return ServiceGenerator.createService(
            endpoint, MobileServerApi::class.java, gson, interceptor, timeout
        )
    }

    @Singleton
    @Provides
    fun provideMobileServerApiInterceptor(): MobileServerApiInterceptor {
        return MobileServerApiInterceptor()
    }

    @Singleton
    @Provides
    fun provideFileCourierApi(
        endpoint: String, timeout: Long, gson: Gson,
        interceptor: FileCourierServerInterceptor
    ): FileCourierServerApi {
        return ServiceGenerator.createService(
            endpoint, FileCourierServerApi::class.java, gson, interceptor,
            timeout
        )
    }

    @Singleton
    @Provides
    fun provideFileCourierApiInterceptor(): FileCourierServerInterceptor {
        return FileCourierServerInterceptor()
    }
}