package com.bitmark.registry.feature.main

import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel

class MainActivity : BaseAppCompatActivity() {

    override fun layoutRes(): Int = R.layout.activity_main

    override fun viewModel(): BaseViewModel? = null
}
