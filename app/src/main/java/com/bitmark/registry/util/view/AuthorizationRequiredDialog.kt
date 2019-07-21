package com.bitmark.registry.util.view

import android.content.Context
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import kotlinx.android.synthetic.main.layout_authorization.*


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AuthorizationRequiredDialog(
    context: Context,
    private val authorizeEvent: () -> Unit
) :
    BaseAppCompatDialog(context) {

    override fun layoutRes(): Int = R.layout.layout_authorization

    override fun initComponents() {
        super.initComponents()
        setCancelable(false)
        btnAuthorize.setSafetyOnclickListener { authorizeEvent.invoke() }
    }
}