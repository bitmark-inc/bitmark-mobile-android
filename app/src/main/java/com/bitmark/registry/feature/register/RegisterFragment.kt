package com.bitmark.registry.feature.register

import android.os.Bundle
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
        private const val URI = "uri"

        fun newInstance(uri: String? = null): RegisterFragment {
            val bundle = Bundle()
            if (uri != null) bundle.putString(URI, uri)
            val fragment = RegisterFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    internal lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_register

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()
        val uri = arguments?.getString(URI)

        btnAccessAccount.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT)
                .replaceFragment(
                    R.id.layoutContainer,
                    RecoveryPhraseSigninFragment.newInstance(uri)
                )
        }

        btnCreateAccount.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT)
                .replaceFragment(
                    R.id.layoutContainer,
                    AuthenticationFragment.newInstance(uri = uri)
                )
        }
    }


}