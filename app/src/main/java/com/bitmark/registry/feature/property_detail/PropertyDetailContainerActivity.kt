package com.bitmark.registry.feature.property_detail

import android.os.Bundle
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.modelview.BitmarkModelView
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-20
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertyDetailContainerActivity : BaseAppCompatActivity() {

    companion object {
        const val BITMARK = "BITMARK"

        fun getBundle(bitmark: BitmarkModelView): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(BITMARK, bitmark)
            return bundle
        }
    }

    @Inject
    lateinit var navigator: Navigator

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val bitmark = intent.extras?.getParcelable<BitmarkModelView>(BITMARK)!!
        navigator.replaceFragment(
            R.id.layoutContainer,
            PropertyDetailFragment.newInstance(bitmark),
            false
        )
    }

    override fun layoutRes(): Int = R.layout.activity_property_detail_container

    override fun viewModel(): BaseViewModel? = null

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.layoutContainer)
        if (currentFragment as? PropertyDetailFragment != null) {
            navigator.anim(Navigator.RIGHT_LEFT).finishActivity()
            super.onBackPressed()
        } else {
            (currentFragment as? BaseSupportFragment)?.onBackPressed()
        }
    }
}