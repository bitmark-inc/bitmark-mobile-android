package com.bitmark.registry.util.view

import android.content.Context
import com.bitmark.registry.R
import kotlinx.android.synthetic.main.layout_progress_dialog.*


/**
 * @author Hieu Pham
 * @since 2019-07-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ProgressAppCompatDialog(
    context: Context,
    private val title: String? = "",
    private val message: String? = ""
) :
    BaseAppCompatDialog(context) {

    override fun layoutRes(): Int = R.layout.layout_progress_dialog

    override fun initComponents() {
        super.initComponents()
        setCancelable(false)
        tvTitle.text = title
        tvMessage.text = message
    }

    fun setProgress(percent: Int) {
        progressBar.progress = percent
    }
}