package com.bitmark.registry.di

import com.bitmark.registry.feature.main.account.AccountFragment
import com.bitmark.registry.feature.main.account.AccountModule
import com.bitmark.registry.feature.main.account.authorized.AuthorizedFragment
import com.bitmark.registry.feature.main.account.authorized.AuthorizedModule
import com.bitmark.registry.feature.main.account.settings.SettingsFragment
import com.bitmark.registry.feature.main.account.settings.SettingsModule
import com.bitmark.registry.feature.main.properties.PropertiesFragment
import com.bitmark.registry.feature.main.properties.PropertiesModule
import com.bitmark.registry.feature.main.properties.yours.YourPropertiesFragment
import com.bitmark.registry.feature.main.properties.yours.YourPropertiesModule
import com.bitmark.registry.feature.main.transactions.TransactionsFragment
import com.bitmark.registry.feature.main.transactions.TransactionsModule
import com.bitmark.registry.feature.main.transactions.action_required.ActionRequiredFragment
import com.bitmark.registry.feature.main.transactions.action_required.ActionRequiredModule
import com.bitmark.registry.feature.main.transactions.history.TransactionHistoryFragment
import com.bitmark.registry.feature.main.transactions.history.TransactionHistoryModule
import com.bitmark.registry.feature.property_detail.PropertyDetailFragment
import com.bitmark.registry.feature.property_detail.PropertyDetailModule
import com.bitmark.registry.feature.register.RegisterFragment
import com.bitmark.registry.feature.register.RegisterModule
import com.bitmark.registry.feature.register.authentication.AuthenticationFragment
import com.bitmark.registry.feature.register.authentication.AuthenticationModule
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseSigninFragment
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseSigninModule
import com.bitmark.registry.feature.transfer.TransferFragment
import com.bitmark.registry.feature.transfer.TransferModule
import dagger.Module
import dagger.android.ContributesAndroidInjector


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
abstract class FragmentBuilderModule {

    @ContributesAndroidInjector(modules = [PropertiesModule::class])
    @FragmentScope
    internal abstract fun bindPropertiesFragment(): PropertiesFragment

    @ContributesAndroidInjector(modules = [TransactionsModule::class])
    @FragmentScope
    internal abstract fun bindTransactionsFragment(): TransactionsFragment

    @ContributesAndroidInjector(modules = [AccountModule::class])
    @FragmentScope
    internal abstract fun bindAccountFragment(): AccountFragment

    @ContributesAndroidInjector(modules = [YourPropertiesModule::class])
    @FragmentScope
    internal abstract fun bindYourPropertiesFragment(): YourPropertiesFragment

    @ContributesAndroidInjector(modules = [PropertyDetailModule::class])
    @FragmentScope
    internal abstract fun bindPropertyDetailFragment(): PropertyDetailFragment

    @ContributesAndroidInjector(modules = [TransferModule::class])
    @FragmentScope
    internal abstract fun bindTransferFragment(): TransferFragment

    @ContributesAndroidInjector(modules = [ActionRequiredModule::class])
    @FragmentScope
    internal abstract fun bindActionRequiredFragment(): ActionRequiredFragment

    @ContributesAndroidInjector(modules = [TransactionHistoryModule::class])
    @FragmentScope
    internal abstract fun bindTransactionHistoryFragment(): TransactionHistoryFragment

    @ContributesAndroidInjector(modules = [SettingsModule::class])
    @FragmentScope
    internal abstract fun bindAccountSettingsFragment(): SettingsFragment

    @ContributesAndroidInjector(modules = [AuthorizedModule::class])
    @FragmentScope
    internal abstract fun bindAccountAuthorizedFragment(): AuthorizedFragment

    @ContributesAndroidInjector(modules = [RecoveryPhraseSigninModule::class])
    @FragmentScope
    internal abstract fun bindRecoverySigninFragment(): RecoveryPhraseSigninFragment

    @ContributesAndroidInjector(modules = [AuthenticationModule::class])
    @FragmentScope
    internal abstract fun bindAuthenticationFragment(): AuthenticationFragment

    @ContributesAndroidInjector(modules = [RegisterModule::class])
    @FragmentScope
    internal abstract fun bindRegisterFragment(): RegisterFragment
}