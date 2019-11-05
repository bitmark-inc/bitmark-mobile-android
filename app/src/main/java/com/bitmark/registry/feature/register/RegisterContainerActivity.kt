/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.register

import android.net.Uri
import android.os.Bundle
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseSigninFragment
import javax.inject.Inject

class RegisterContainerActivity : BaseAppCompatActivity() {

    companion object {
        private const val URI = "uri"

        private const val RECOVER_ACCOUNT = "recover_account"

        fun getBundle(
            uri: Uri? = null,
            recoverAccount: Boolean = false
        ): Bundle {
            val bundle = Bundle()
            if (uri != null) bundle.putString(URI, uri.toString())
            bundle.putBoolean(RECOVER_ACCOUNT, recoverAccount)
            return bundle
        }
    }

    @Inject
    lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.activity_register_container

    override fun viewModel(): BaseViewModel? = null

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val recoverAccount =
            intent?.extras?.getBoolean(RECOVER_ACCOUNT) ?: false
        val uri = intent?.extras?.getString(URI)
        val fragment =
            if (recoverAccount) {
                RecoveryPhraseSigninFragment.newInstance(uri, true)
            } else {
                RegisterFragment.newInstance(uri)
            }
        navigator.replaceFragment(R.id.layoutContainer, fragment, false)
    }
}