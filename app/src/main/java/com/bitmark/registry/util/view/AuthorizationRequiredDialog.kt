/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.view

import android.content.Context
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import kotlinx.android.synthetic.main.layout_authorization.*

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