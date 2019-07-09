package com.bitmark.registry.feature.register

import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.register.authentication.AuthenticationActivity
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseSigninActivity
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import kotlinx.android.synthetic.main.activity_register.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RegisterActivity : BaseAppCompatActivity() {

    @Inject
    internal lateinit var navigator: Navigator<RegisterActivity>

    override fun layoutRes(): Int = R.layout.activity_register

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()
        btnAccessAccount.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT)
                .startActivity(RecoveryPhraseSigninActivity::class.java)
        }

        btnCreateAccount.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT)
                .startActivity(AuthenticationActivity::class.java)
        }
    }


}