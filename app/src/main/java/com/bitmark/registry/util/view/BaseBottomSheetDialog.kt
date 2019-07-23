package com.bitmark.registry.util.view

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


/**
 * @author Hieu Pham
 * @since 2019-07-23
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
abstract class BaseBottomSheetDialog(context: Context) :
    BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes())
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        initComponents()

        setOnShowListener {
            val bottomSheet =
                findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from<FrameLayout>(bottomSheet)
                .setState(BottomSheetBehavior.STATE_EXPANDED)
        }
    }

    abstract fun layoutRes(): Int

    open protected fun initComponents() {}
}