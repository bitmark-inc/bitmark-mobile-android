package com.bitmark.registry.di

import com.bitmark.registry.feature.account.AccountContainerFragment
import com.bitmark.registry.feature.account.AccountContainerModule
import com.bitmark.registry.feature.account.SettingsFragment
import com.bitmark.registry.feature.account.SettingsModule
import com.bitmark.registry.feature.account.details.SettingsDetailsFragment
import com.bitmark.registry.feature.account.details.SettingsDetailsModule
import com.bitmark.registry.feature.issuance.selection.AssetSelectionFragment
import com.bitmark.registry.feature.issuance.selection.AssetSelectionModule
import com.bitmark.registry.feature.properties.PropertiesContainerFragment
import com.bitmark.registry.feature.properties.PropertiesContainerModule
import com.bitmark.registry.feature.properties.PropertiesFragment
import com.bitmark.registry.feature.properties.PropertiesModule
import com.bitmark.registry.feature.properties.yours.YourPropertiesFragment
import com.bitmark.registry.feature.properties.yours.YourPropertiesModule
import com.bitmark.registry.feature.recoveryphrase.show.RecoveryPhraseShowingFragment
import com.bitmark.registry.feature.recoveryphrase.show.RecoveryPhraseShowingModule
import com.bitmark.registry.feature.recoveryphrase.show.RecoveryPhraseWarningFragment
import com.bitmark.registry.feature.recoveryphrase.show.RecoveryPhraseWarningModule
import com.bitmark.registry.feature.recoveryphrase.test.RecoveryPhraseTestFragment
import com.bitmark.registry.feature.recoveryphrase.test.RecoveryPhraseTestModule
import com.bitmark.registry.feature.register.RegisterFragment
import com.bitmark.registry.feature.register.RegisterModule
import com.bitmark.registry.feature.register.authentication.AuthenticationFragment
import com.bitmark.registry.feature.register.authentication.AuthenticationModule
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseSigninFragment
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseSigninModule
import com.bitmark.registry.feature.transactions.action_required.ActionRequiredFragment
import com.bitmark.registry.feature.transactions.action_required.ActionRequiredModule
import com.bitmark.registry.feature.transactions.history.TransactionHistoryFragment
import com.bitmark.registry.feature.transactions.history.TransactionHistoryModule
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

//    @ContributesAndroidInjector(modules = [TransactionsModule::class])
//    @FragmentScope
//    internal abstract fun bindTransactionsFragment(): TransactionsFragment

    @ContributesAndroidInjector(modules = [YourPropertiesModule::class])
    @FragmentScope
    internal abstract fun bindYourPropertiesFragment(): YourPropertiesFragment

    @ContributesAndroidInjector(modules = [ActionRequiredModule::class])
    @FragmentScope
    internal abstract fun bindActionRequiredFragment(): ActionRequiredFragment

    @ContributesAndroidInjector(modules = [TransactionHistoryModule::class])
    @FragmentScope
    internal abstract fun bindTransactionHistoryFragment(): TransactionHistoryFragment

    @ContributesAndroidInjector(modules = [SettingsModule::class])
    @FragmentScope
    internal abstract fun bindAccountSettingsFragment(): SettingsFragment

    @ContributesAndroidInjector(modules = [RecoveryPhraseSigninModule::class])
    @FragmentScope
    internal abstract fun bindRecoverySigninFragment(): RecoveryPhraseSigninFragment

    @ContributesAndroidInjector(modules = [AuthenticationModule::class])
    @FragmentScope
    internal abstract fun bindAuthenticationFragment(): AuthenticationFragment

    @ContributesAndroidInjector(modules = [RegisterModule::class])
    @FragmentScope
    internal abstract fun bindRegisterFragment(): RegisterFragment

    @ContributesAndroidInjector(modules = [RecoveryPhraseWarningModule::class])
    @FragmentScope
    internal abstract fun bindRecoveryPhraseWarningFragment(): RecoveryPhraseWarningFragment

    @ContributesAndroidInjector(modules = [RecoveryPhraseShowingModule::class])
    @FragmentScope
    internal abstract fun bindRecoveryPhraseShowingFragment(): RecoveryPhraseShowingFragment

    @ContributesAndroidInjector(modules = [RecoveryPhraseTestModule::class])
    @FragmentScope
    internal abstract fun bindRecoveryPhraseTestFragment(): RecoveryPhraseTestFragment

    @ContributesAndroidInjector(modules = [AccountContainerModule::class])
    @FragmentScope
    internal abstract fun bindAccountContainerFragment(): AccountContainerFragment

    @ContributesAndroidInjector(modules = [SettingsDetailsModule::class])
    @FragmentScope
    internal abstract fun bindSettingsDetailsFragment(): SettingsDetailsFragment

    @ContributesAndroidInjector(modules = [AssetSelectionModule::class])
    @FragmentScope
    internal abstract fun bindAssetSelectionFragment(): AssetSelectionFragment

    @ContributesAndroidInjector(modules = [PropertiesContainerModule::class])
    @FragmentScope
    internal abstract fun bindPropertiesContainerFragment(): PropertiesContainerFragment
}