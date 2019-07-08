package com.bitmark.registry.feature.main.properties

import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel


/**
 * @author Hieu Pham
 * @since 7/8/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertiesFragment : BaseSupportFragment() {

    companion object {
        fun newInstance(): PropertiesFragment = PropertiesFragment()
    }

    override fun viewModel(): BaseViewModel? = null

    override fun layoutRes(): Int = R.layout.fragment_properties
}