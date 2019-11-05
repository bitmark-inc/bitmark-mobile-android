/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.issuance.issuance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bitmark.registry.R
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import kotlinx.android.synthetic.main.activity_property_description.*

class PropertyDescriptionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_description)

        ivBack.setOnClickListener {
            Navigator(this).anim(RIGHT_LEFT).finishActivity()
        }
    }

    override fun onBackPressed() {
        Navigator(this).anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }
}