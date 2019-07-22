package com.bitmark.registry.feature.main.transactions

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bitmark.registry.R
import com.bitmark.registry.feature.ViewPagerAdapter
import com.bitmark.registry.feature.main.transactions.action_required.ActionRequiredFragment
import com.bitmark.registry.feature.main.transactions.history.TransactionHistoryFragment


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class TransactionsViewPagerAdapter(
    private val context: Context?,
    fm: FragmentManager
) : ViewPagerAdapter(fm) {
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