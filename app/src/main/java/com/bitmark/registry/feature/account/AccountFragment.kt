package com.bitmark.registry.feature.account

import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_account.*


/**
 * @author Hieu Pham
 * @since 7/8/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AccountFragment : BaseSupportFragment() {

    companion object {
        fun newInstance(): AccountFragment = AccountFragment()
    }

    private lateinit var adapter: AccountViewPagerAdapter

    private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {

        override fun onTabReselected(p0: TabLayout.Tab?) {
            (adapter.currentFragment as? BaseSupportFragment)?.refresh()
        }

        override fun onTabUnselected(p0: TabLayout.Tab?) {
        }

        override fun onTabSelected(p0: TabLayout.Tab?) {
        }

    }

    override fun viewModel(): BaseViewModel? = null

    override fun layoutRes(): Int = R.layout.fragment_account

    override fun initComponents() {
        super.initComponents()

        adapter = AccountViewPagerAdapter(context, childFragmentManager)
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        tabLayout.addOnTabSelectedListener(tabSelectedListener)
    }

    override fun deinitComponents() {
        tabLayout.removeOnTabSelectedListener(tabSelectedListener)
        super.deinitComponents()
    }

    override fun refresh() {
        super.refresh()
        viewPager.currentItem = 0
    }
}