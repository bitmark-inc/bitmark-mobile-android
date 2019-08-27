package com.bitmark.registry

import android.util.Log
import com.bitmark.apiservice.configuration.GlobalConfiguration
import com.bitmark.apiservice.configuration.Network
import com.bitmark.registry.data.source.remote.api.service.ServiceGenerator
import com.bitmark.registry.feature.connectivity.ConnectivityHandler
import com.bitmark.registry.keymanagement.ApiKeyManager.Companion.API_KEY_MANAGER
import com.bitmark.sdk.features.BitmarkSDK
import com.crashlytics.android.Crashlytics
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import io.fabric.sdk.android.Fabric
import io.intercom.android.sdk.Intercom
import io.reactivex.plugins.RxJavaPlugins
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RegistryApplication : DaggerApplication() {

    companion object {
        private const val TAG = "RegistryApplication"
    }

    @Inject
    lateinit var appLifecycleHandler: AppLifecycleHandler

    @Inject
    lateinit var connectivityHandler: ConnectivityHandler

    private val applicationInjector = DaggerAppComponent.builder()
        .application(this)
        .build()

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return applicationInjector
    }

    override fun onCreate() {
        super.onCreate()
        BitmarkSDK.init(buildBmSdkConfig())
        Fabric.with(this, Crashlytics())
        Intercom.initialize(this, API_KEY_MANAGER.intercomApiKey, "ejkeunzw")
        registerActivityLifecycleCallbacks(appLifecycleHandler)
        RxJavaPlugins.setErrorHandler { e ->
            Log.e(
                TAG,
                "intercept rx error ${e.javaClass} with message ${e.message} to be sent to thread uncaught exception"
            )
        }
        connectivityHandler.register()
    }

    private fun buildBmSdkConfig(): GlobalConfiguration.Builder {
        val builder = GlobalConfiguration.builder().withConnectionTimeout(
            ServiceGenerator.CONNECTION_TIMEOUT.toInt()
        )
        if (BuildConfig.DEBUG) {
            builder.withLogLevel(HttpLoggingInterceptor.Level.BODY)
        }
        if ("prd".equals(BuildConfig.FLAVOR)) {
            builder.withApiToken(API_KEY_MANAGER.bitmarkApiKey)
                .withNetwork(Network.LIVE_NET)
        } else {
            builder.withApiToken("bmk-lljpzkhqdkzmblhg")
                .withNetwork(Network.TEST_NET)
        }
        return builder
    }
}