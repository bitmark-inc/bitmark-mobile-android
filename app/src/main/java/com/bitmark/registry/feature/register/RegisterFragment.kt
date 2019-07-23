package com.bitmark.registry.feature.register

import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.register.authentication.AuthenticationFragment
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseSigninFragment
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import kotlinx.android.synthetic.main.fragment_register.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RegisterFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() = RegisterFragment()
    }

    @Inject
    internal lateinit var navigator: Navigator<RegisterFragment>

    override fun layoutRes(): Int = R.layout.fragment_register

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()
        btnAccessAccount.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT)
                .replaceFragment(
                    R.id.layoutContainer,
                    RecoveryPhraseSigninFragment.newInstance()
                )
        }

        btnCreateAccount.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT)
                .replaceFragment(
                    R.id.layoutContainer,
                    AuthenticationFragment.newInstance()
                )
        }
    }


}