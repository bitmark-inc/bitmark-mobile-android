package com.bitmark.registry.feature.main.transactions.action_required

import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ActionRequiredFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() = ActionRequiredFragment()
    }

    override fun layoutRes(): Int = R.layout.fragment_action_required

    override fun viewModel(): BaseViewModel? = null
}