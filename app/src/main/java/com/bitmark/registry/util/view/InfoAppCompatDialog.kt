/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.view

import android.content.Context
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.visible
import kotlinx.android.synthetic.main.layout_info_dialog.*

class InfoAppCompatDialog(
    context: Context,
    private val message: String,
    private val legendMessage: String? = null,
    private val dismissListener: (() -> Unit)? = null
    ) : BaseAppCompatDialog(context) {

    override fun layoutRes(): Int = R.layout.layout_info_dialog

    override fun initComponents() {
        super.initComponents()
        setCancelable(false)
        if (legendMessage != null) {
            tvLegendMessage.visible()
            tvLegendMessage.text = legendMessage
        } else {
            tvLegendMessage.gone()
        }

        tvPrimaryMessage.text = message

        setOnDismissListener { dismissListener?.invoke() }
    }
}