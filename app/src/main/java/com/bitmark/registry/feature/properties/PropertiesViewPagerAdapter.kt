package com.bitmark.registry.feature.properties

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.ViewPagerAdapter
import com.bitmark.registry.feature.WebViewFragment
import com.bitmark.registry.feature.properties.yours.YourPropertiesFragment


/**
 * @author Hieu Pham
 * @since 2019-07-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertiesViewPagerAdapter(
    private val context: Context?,
    fm: FragmentManager
) : ViewPagerAdapter(fm) {

    companion object {
        const val TAB_YOUR = 0x00
        const val TAB_GLOBAL = 0x01
    }

    init {
        super.add(
            YourPropertiesFragment.newInstance(),
            WebViewFragment.newInstance(
                "%s?env=app".format(BuildConfig.REGISTRY_WEBSITE),
                hasNav = true
            )
        )
    }

    override fun add(vararg fragments: Fragment) {
        throw UnsupportedOperationException("not support")
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> context?.getString(R.string.yours) ?: ""
            1 -> context?.getString(R.string.global) ?: ""
            else -> throw RuntimeException("invalid fragment declaration")
        }
    }
}