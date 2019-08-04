package com.bitmark.registry.feature.properties

import android.content.Intent
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.BehaviorComponent
import com.bitmark.registry.feature.Navigator
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-30
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertiesContainerFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() = PropertiesContainerFragment()
    }

    @Inject
    lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_properties_container

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()
        navigator.replaceChildFragment(
            R.id.layoutContainer,
            PropertiesFragment.newInstance(), false
        )
    }

    override fun onBackPressed(): Boolean {
        super.onBackPressed()
        val currentFragment = currentFragment() as? BehaviorComponent
            ?: return false
        return currentFragment.onBackPressed()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        currentFragment()?.onActivityResult(requestCode, resultCode, data)
            ?: super.onActivityResult(requestCode, resultCode, data)
    }

    override fun refresh() {
        super.refresh()
        (currentFragment() as? BehaviorComponent)?.refresh()
    }

    private fun currentFragment() =
        childFragmentManager.findFragmentById(R.id.layoutContainer)
}