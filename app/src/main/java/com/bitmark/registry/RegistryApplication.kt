package com.bitmark.registry

import com.bitmark.apiservice.configuration.GlobalConfiguration
import com.bitmark.apiservice.configuration.Network
import com.bitmark.registry.keymanagement.ApiKeyManager.Companion.API_KEY_MANAGER
import com.bitmark.sdk.features.BitmarkSDK
import com.crashlytics.android.Crashlytics
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import io.fabric.sdk.android.Fabric
import io.intercom.android.sdk.Intercom
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RegistryApplication : DaggerApplication() {

    @Inject
    lateinit var appLifecycleHandler: AppLifecycleHandler

    private val applicationInjector = DaggerAppComponent.builder()
        .application(this)
        .build()

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return applicationInjector
    }

    override fun onCreate() {
        super.onCreate()
        if ("prd".equals(BuildConfig.FLAVOR)) {
            val builder = GlobalConfiguration.builder()
                .withApiToken(API_KEY_MANAGER.bitmarkApiKey)
                .withNetwork(Network.LIVE_NET)
            BitmarkSDK.init(builder)
        } else {
            BitmarkSDK.init("bmk-lljpzkhqdkzmblhg")
        }
        Fabric.with(this, Crashlytics())
        Intercom.initialize(this, API_KEY_MANAGER.intercomApiKey, "ejkeunzw")
        registerActivityLifecycleCallbacks(appLifecycleHandler)
    }
}