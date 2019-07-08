package com.bitmark.registry.feature.main.account

import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel


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

    override fun viewModel(): BaseViewModel? = null

    override fun layoutRes(): Int = R.layout.fragment_account
}