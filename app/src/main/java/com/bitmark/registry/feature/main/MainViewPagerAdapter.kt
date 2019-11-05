/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bitmark.registry.feature.ViewPagerAdapter
import com.bitmark.registry.feature.account.AccountContainerFragment
import com.bitmark.registry.feature.properties.PropertiesContainerFragment
import com.bitmark.registry.feature.transactions.history.TransactionHistoryFragment

class MainViewPagerAdapter(fm: FragmentManager) : ViewPagerAdapter(fm) {

    companion object {
        const val TAB_PROPERTIES = 0x00
        const val TAB_TXS = 0x01
        const val TAB_ACCOUNT = 0x02
    }

    init {
        super.add(
            PropertiesContainerFragment.newInstance(),
            TransactionHistoryFragment.newInstance(),
            AccountContainerFragment.newInstance()
        )
    }

    override fun add(vararg fragments: Fragment) {
        throw UnsupportedOperationException("not support")
    }

}