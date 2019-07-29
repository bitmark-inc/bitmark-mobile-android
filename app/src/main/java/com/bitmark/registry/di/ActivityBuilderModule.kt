package com.bitmark.registry.di

import com.bitmark.registry.feature.account.settings.details.WhatsNewActivity
import com.bitmark.registry.feature.account.settings.details.WhatsNewModule
import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.feature.main.MainModule
import com.bitmark.registry.feature.property_detail.PropertyDetailContainerActivity
import com.bitmark.registry.feature.property_detail.PropertyDetailContainerModule
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.feature.register.RegisterContainerModule
import com.bitmark.registry.feature.scan_qr_code.ScanQrCodeActivity
import com.bitmark.registry.feature.scan_qr_code.ScanQrCodeModule
import com.bitmark.registry.feature.splash.SplashActivity
import com.bitmark.registry.feature.splash.SplashModule
import dagger.Module
import dagger.android.ContributesAndroidInjector


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
abstract class ActivityBuilderModule {

    @ContributesAndroidInjector(modules = [SplashModule::class])
    @ActivityScope
    internal abstract fun bindSplashActivity(): SplashActivity

    @ContributesAndroidInjector(modules = [RegisterContainerModule::class])
    @ActivityScope
    internal abstract fun bindRegisterContainerActivity(): RegisterContainerActivity

    @ContributesAndroidInjector(modules = [MainModule::class])
    @ActivityScope
    internal abstract fun bindMainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [PropertyDetailContainerModule::class])
    @ActivityScope
    internal abstract fun bindPropertyDetailContainerActivity(): PropertyDetailContainerActivity

    @ContributesAndroidInjector(modules = [ScanQrCodeModule::class])
    @ActivityScope
    internal abstract fun bindScanQrCodeActivity(): ScanQrCodeActivity

    @ContributesAndroidInjector(modules = [WhatsNewModule::class])
    @ActivityScope
    internal abstract fun bindWhatsNewActivity(): WhatsNewActivity
}