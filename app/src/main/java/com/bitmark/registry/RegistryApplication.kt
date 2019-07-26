package com.bitmark.registry

import com.bitmark.registry.keymanagement.ApiKeyManager.Companion.API_KEY_MANAGER
import com.bitmark.sdk.features.BitmarkSDK
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import io.intercom.android.sdk.Intercom


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RegistryApplication : DaggerApplication() {

    private val applicationInjector = DaggerAppComponent.builder()
        .application(this)
        .build()

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return applicationInjector
    }

    override fun onCreate() {
        super.onCreate()
        if ("prd".equals(BuildConfig.FLAVOR)) {
            BitmarkSDK.init(API_KEY_MANAGER.bitmarkApiKey)
        } else {
            BitmarkSDK.init("bmk-lljpzkhqdkzmblhg")
        }
        Intercom.initialize(this, API_KEY_MANAGER.intercomApiKey, "ejkeunzw")
    }
}