package com.bitmark.registry.feature.issuance.issuance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bitmark.registry.R
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import kotlinx.android.synthetic.main.activity_property_description.*


/**
 * @author Hieu Pham
 * @since 2019-08-18
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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