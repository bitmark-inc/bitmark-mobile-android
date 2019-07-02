package com.bitmark.registry.data.source.remote.api.service

import androidx.annotation.VisibleForTesting
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.source.remote.api.middleware.RxErrorHandlingCallAdapterFactory
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ServiceGenerator {

    companion object {

        const val CONNECTION_TIMEOUT = 15L

        @VisibleForTesting
        const val TEST_CONNECTION_TIMEOUT = 2L

        fun <T> createService(
            endPoint: String, serviceClass: Class<T>, gson: Gson,
            interceptor: Interceptor?
        ): T {
            return createService(
                endPoint, serviceClass, gson, interceptor, CONNECTION_TIMEOUT
            )
        }

        fun <T> createService(
            endPoint: String, serviceClass: Class<T>, gson: Gson,
            interceptor: Interceptor?, timeout: Long
        ): T {
            val httpClientBuilder = OkHttpClient.Builder()
            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                httpClientBuilder.addInterceptor(loggingInterceptor)
            }
            if (interceptor != null) {
                httpClientBuilder.addInterceptor(interceptor)
            }
            httpClientBuilder.writeTimeout(timeout, TimeUnit.SECONDS)
            httpClientBuilder.readTimeout(timeout, TimeUnit.SECONDS)
            httpClientBuilder.connectTimeout(timeout, TimeUnit.SECONDS)
            val builder = Retrofit.Builder().baseUrl(endPoint)
                .addCallAdapterFactory(RxErrorHandlingCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
            val retrofit = builder.client(httpClientBuilder.build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
            return retrofit.create(serviceClass)
        }
    }
}