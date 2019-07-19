package com.bitmark.registry.data.source.remote.api

import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.source.remote.api.middleware.CoreApiInterceptor
import com.bitmark.registry.data.source.remote.api.middleware.FileCourierServerInterceptor
import com.bitmark.registry.data.source.remote.api.middleware.MobileServerApiInterceptor
import com.bitmark.registry.data.source.remote.api.service.*
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
        return GsonBuilder().excludeFieldsWithoutExposeAnnotation().setLenient()
            .create()
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

    @Singleton
    @Provides
    fun provideKeyAccountServerApi(gson: Gson): KeyAccountServerApi {
        return ServiceGenerator.createService(
            BuildConfig.KEY_ACCOUNT_SERVER_ENDPOINT,
            KeyAccountServerApi::class.java,
            gson, null
        )
    }
}