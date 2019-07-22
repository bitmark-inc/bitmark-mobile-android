package com.bitmark.registry.di

import com.bitmark.registry.feature.main.account.AccountFragment
import com.bitmark.registry.feature.main.account.AccountModule
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
}