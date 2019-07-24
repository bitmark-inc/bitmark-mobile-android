package com.bitmark.registry.feature.register

import android.os.Bundle
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-23
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RegisterContainerActivity : BaseAppCompatActivity() {

    @Inject
    lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.activity_register_container

    override fun viewModel(): BaseViewModel? = null

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        navigator.replaceFragment(
            R.id.layoutContainer,
            RegisterFragment.newInstance(),
            false
        )
    }
}