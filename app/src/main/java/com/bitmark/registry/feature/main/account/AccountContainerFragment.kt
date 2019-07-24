package com.bitmark.registry.feature.main.account

import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-24
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AccountContainerFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() = AccountContainerFragment()
    }

    @Inject
    lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_account_container

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()
        navigator.replaceChildFragment(
            R.id.layoutContainer,
            AccountFragment.newInstance(), false
        )
    }

    override fun onBackPressed(): Boolean {
        super.onBackPressed()
        val currentFragment =
            childFragmentManager.findFragmentById(R.id.layoutContainer) as? BaseSupportFragment
                ?: return false
        return currentFragment.onBackPressed()
    }
}