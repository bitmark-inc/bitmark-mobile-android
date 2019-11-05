/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.view

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialog
import com.bitmark.registry.R

abstract class BaseAppCompatDialog(context: Context) :
    AppCompatDialog(
        context,
        R.style.Theme_AppCompat_Light_NoActionBar_Translucent
    ) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes())
        initComponents()
    }

    abstract fun layoutRes(): Int

    open fun initComponents() {}
}