package com.bitmark.registry.util.view

import android.content.Context
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.visible
import kotlinx.android.synthetic.main.layout_progress_dialog.*


/**
 * @author Hieu Pham
 * @since 2019-07-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ProgressAppCompatDialog(
    context: Context,
    private val title: String? = null,
    private val message: String,
    private val indeterminate: Boolean = false
) :
    BaseAppCompatDialog(context) {

    override fun layoutRes(): Int =
        if (indeterminate) R.layout.layout_progress_dialog_2 else R.layout.layout_progress_dialog

    override fun initComponents() {
        super.initComponents()
        setCancelable(false)
        if (title != null) {
            tvTitle.text = title
            tvTitle.visible()
        } else {
            tvTitle.gone()
        }
        tvMessage.text = message
    }

    fun setProgress(percent: Int) {
        if (indeterminate) throw UnsupportedOperationException("could not set progress in indeterminate mode")
        progressBar.progress = percent
    }
}