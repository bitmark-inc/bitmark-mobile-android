package com.bitmark.registry.feature.main.account

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bitmark.registry.R
import com.bitmark.registry.feature.ViewPagerAdapter
import com.bitmark.registry.feature.main.account.authorized.AuthorizedFragment
import com.bitmark.registry.feature.main.account.settings.SettingsFragment


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AccountViewPagerAdapter(
    private val context: Context?,
    fm: FragmentManager
) : ViewPagerAdapter(fm) {
    init {
        super.add(
            SettingsFragment.newInstance(),
            AuthorizedFragment.newInstance()
        )
    }

    override fun add(vararg fragments: Fragment) {
        throw UnsupportedOperationException("not support")
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> context?.getString(R.string.settings) ?: ""
            1 -> context?.getString(R.string.authorized) ?: ""
            else -> throw RuntimeException("invalid fragment declaration")
        }
    }
}