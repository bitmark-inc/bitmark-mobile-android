package com.bitmark.registry.feature.main.account.authorized

import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AuthorizedFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() = AuthorizedFragment()
    }

    override fun layoutRes(): Int = R.layout.fragment_authorized

    override fun viewModel(): BaseViewModel? = null
}