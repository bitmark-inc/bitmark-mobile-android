package com.bitmark.registry.feature.account

import android.os.Handler
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.BehaviorComponent
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.recoveryphrase.show.RecoveryPhraseShowingFragment
import com.bitmark.registry.feature.recoveryphrase.show.RecoveryPhraseWarningFragment
import com.bitmark.registry.feature.recoveryphrase.test.RecoveryPhraseTestFragment
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-24
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AccountContainerFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() = AccountContainerFragment()
    }

    @Inject
    lateinit var navigator: Navigator

    private val handler = Handler()

    override fun layoutRes(): Int = R.layout.fragment_account_container

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()
        navigator.replaceChildFragment(
            R.id.layoutContainer,
            SettingsFragment.newInstance(), false
        )
    }

    override fun deinitComponents() {
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
    }

    override fun onBackPressed(): Boolean {
        super.onBackPressed()
        val currentFragment = currentFragment() as? BehaviorComponent
            ?: return false
        return currentFragment.onBackPressed()
    }

    fun gotoRecoveryPhraseWarning() {
        val currentFragment = currentFragment()


        if (currentFragment !is RecoveryPhraseWarningFragment
            && currentFragment !is RecoveryPhraseShowingFragment
            && currentFragment !is RecoveryPhraseTestFragment
        ) {
            if (currentFragment !is SettingsFragment) navigator.popChildFragmentToRoot()

            navigator.anim(Navigator.RIGHT_LEFT).replaceChildFragment(
                R.id.layoutContainer,
                RecoveryPhraseWarningFragment.newInstance(
                    getString(R.string.recovery_phrase),
                    getString(R.string.your_recovery_phrase_is_the_only)
                )
            )
        }

    }

    fun openIntercom() {
        if (currentFragment() !is SettingsFragment) navigator.popChildFragmentToRoot()
        handler.postDelayed(
            { (currentFragment() as? SettingsFragment)?.openIntercom() },
            100
        )
    }

    override fun refresh() {
        super.refresh()
        (currentFragment() as? BehaviorComponent)?.refresh()
    }

    private fun currentFragment() =
        childFragmentManager.findFragmentById(R.id.layoutContainer)
}