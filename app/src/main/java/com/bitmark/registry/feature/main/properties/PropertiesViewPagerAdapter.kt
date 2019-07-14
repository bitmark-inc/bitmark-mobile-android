package com.bitmark.registry.feature.main.properties

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.ViewPagerAdapter
import com.bitmark.registry.feature.WebViewFragment
import com.bitmark.registry.feature.main.properties.yours.YourPropertiesFragment


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

    init {
        super.add(
            YourPropertiesFragment.newInstance(), WebViewFragment.newInstance(
                BuildConfig.REGISTRY_WEBSITE
            )
        )
    }

    override fun add(vararg fragments: Fragment) {
        throw UnsupportedOperationException("not support")
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> context?.getString(R.string.yours)?.toUpperCase() ?: ""
            1 -> context?.getString(R.string.global)?.toUpperCase() ?: ""
            else -> throw RuntimeException("invalid fragment declaration")
        }
    }
}