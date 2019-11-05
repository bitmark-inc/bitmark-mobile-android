/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.di

import com.bitmark.registry.feature.account.details.WhatsNewActivity
import com.bitmark.registry.feature.account.details.WhatsNewModule
import com.bitmark.registry.feature.cloud_service_sign_in.CloudServiceSignInActivity
import com.bitmark.registry.feature.cloud_service_sign_in.CloudServiceSignInModule
import com.bitmark.registry.feature.issuance.issuance.IssuanceActivity
import com.bitmark.registry.feature.issuance.issuance.IssuanceModule
import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.feature.main.MainModule
import com.bitmark.registry.feature.music_claiming.MusicClaimingActivity
import com.bitmark.registry.feature.music_claiming.MusicClaimingModule
import com.bitmark.registry.feature.partner_authorization.PartnerAuthorizationActivity
import com.bitmark.registry.feature.partner_authorization.PartnerAuthorizationModule
import com.bitmark.registry.feature.property_detail.PropertyDetailActivity
import com.bitmark.registry.feature.property_detail.PropertyDetailModule
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.feature.register.RegisterContainerModule
import com.bitmark.registry.feature.scan_qr_code.ScanQrCodeActivity
import com.bitmark.registry.feature.scan_qr_code.ScanQrCodeModule
import com.bitmark.registry.feature.splash.SplashActivity
import com.bitmark.registry.feature.splash.SplashModule
import com.bitmark.registry.feature.transfer.TransferActivity
import com.bitmark.registry.feature.transfer.TransferModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

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

    @ContributesAndroidInjector(modules = [PropertyDetailModule::class])
    @ActivityScope
    internal abstract fun bindPropertyDetailActivity(): PropertyDetailActivity

    @ContributesAndroidInjector(modules = [TransferModule::class])
    @ActivityScope
    internal abstract fun bindTransferActivity(): TransferActivity

    @ContributesAndroidInjector(modules = [ScanQrCodeModule::class])
    @ActivityScope
    internal abstract fun bindScanQrCodeActivity(): ScanQrCodeActivity

    @ContributesAndroidInjector(modules = [WhatsNewModule::class])
    @ActivityScope
    internal abstract fun bindWhatsNewActivity(): WhatsNewActivity

    @ContributesAndroidInjector(modules = [IssuanceModule::class])
    @ActivityScope
    internal abstract fun bindIssuanceActivity(): IssuanceActivity

    @ContributesAndroidInjector(modules = [PartnerAuthorizationModule::class])
    @ActivityScope
    internal abstract fun bindPartnerAuthorizationActivity(): PartnerAuthorizationActivity

    @ContributesAndroidInjector(modules = [MusicClaimingModule::class])
    @ActivityScope
    internal abstract fun bindMusicClaimingActivity(): MusicClaimingActivity

    @ContributesAndroidInjector(modules = [CloudServiceSignInModule::class])
    @ActivityScope
    internal abstract fun bindCloudServiceAuthorizationActivity(): CloudServiceSignInActivity
}