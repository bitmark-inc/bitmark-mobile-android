/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.transactions

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bitmark.registry.R
import com.bitmark.registry.feature.ViewPagerAdapter
import com.bitmark.registry.feature.transactions.action_required.ActionRequiredFragment
import com.bitmark.registry.feature.transactions.history.TransactionHistoryFragment

class TransactionsViewPagerAdapter(
    private val context: Context?,
    fm: FragmentManager
) : ViewPagerAdapter(fm) {

    companion object {
        const val TAB_ACTION_REQUIRED = 0x00
        const val TAB_TXS_HISTORY = 0x01
    }

    init {
        super.add(
            ActionRequiredFragment.newInstance(),
            TransactionHistoryFragment.newInstance()
        )
    }

    override fun add(vararg fragments: Fragment) {
        throw UnsupportedOperationException("not support")
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> context?.getString(R.string.actions_required) ?: ""
            1 -> context?.getString(R.string.history) ?: ""
            else -> throw RuntimeException("invalid fragment declaration")
        }
    }
}