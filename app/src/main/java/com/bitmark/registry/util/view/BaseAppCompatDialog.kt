package com.bitmark.registry.util.view

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialog


/**
 * @author Hieu Pham
 * @since 2019-07-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
abstract class BaseAppCompatDialog(context: Context) :
    AppCompatDialog(
        context,
        android.R.style.Theme_Translucent_NoTitleBar_Fullscreen
    ) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes())
        initComponents()
    }

    abstract fun layoutRes(): Int

    open fun initComponents() {}
}