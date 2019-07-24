package com.bitmark.registry.feature.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bitmark.registry.feature.ViewPagerAdapter
import com.bitmark.registry.feature.main.account.AccountContainerFragment
import com.bitmark.registry.feature.main.properties.PropertiesFragment
import com.bitmark.registry.feature.main.transactions.TransactionsFragment


/**
 * @author Hieu Pham
 * @since 2019-07-24
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MainViewPagerAdapter(fm: FragmentManager) : ViewPagerAdapter(fm) {

    init {
        super.add(
            PropertiesFragment.newInstance(),
            TransactionsFragment.newInstance(),
            AccountContainerFragment.newInstance()
        )
    }

    override fun add(vararg fragments: Fragment) {
        throw UnsupportedOperationException("not support")
    }

}