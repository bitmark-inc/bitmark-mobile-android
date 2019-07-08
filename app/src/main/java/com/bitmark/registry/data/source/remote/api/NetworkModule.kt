package com.bitmark.registry.data.source.remote.api

import com.bitmark.registry.BuildConfig
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
        gson: Gson, interceptor: CoreApiInterceptor
    ): CoreApi {
        return ServiceGenerator.createService(
            BuildConfig.CORE_API_ENDPOINT, CoreApi::class.java, gson,
            interceptor
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
        gson: Gson, interceptor: MobileServerApiInterceptor
    ): MobileServerApi {
        return ServiceGenerator.createService(
            BuildConfig.MOBILE_SERVER_EMPOINT, MobileServerApi::class.java,
            gson, interceptor
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
        gson: Gson, interceptor: FileCourierServerInterceptor
    ): FileCourierServerApi {
        return ServiceGenerator.createService(
            BuildConfig.FILE_COURIER_SERVER_ENPOINT,
            FileCourierServerApi::class.java, gson, interceptor
        )
    }

    @Singleton
    @Provides
    fun provideFileCourierApiInterceptor(): FileCourierServerInterceptor {
        return FileCourierServerInterceptor()
    }
}