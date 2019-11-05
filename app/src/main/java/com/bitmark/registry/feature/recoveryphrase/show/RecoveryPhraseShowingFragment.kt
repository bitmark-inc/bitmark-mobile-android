/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.recoveryphrase.show

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.recoveryphrase.test.RecoveryPhraseTestFragment
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseAdapter
import com.bitmark.registry.util.extension.*
import kotlinx.android.synthetic.main.fragment_recovery_phrase_signin.rvRecoveryPhrase
import kotlinx.android.synthetic.main.fragment_recovery_showing.*
import javax.inject.Inject

class RecoveryPhraseShowingFragment : BaseSupportFragment() {


    companion object {
        private const val RECOVERY_PHRASE = "recovery_phrase"
        private const val REMOVE_ACCESS = "remove_access"

        fun newInstance(
            recoveryPhrase: Array<String>,
            removeAccess: Boolean = false
        ): RecoveryPhraseShowingFragment {
            val bundle = Bundle()
            bundle.putStringArray(RECOVERY_PHRASE, recoveryPhrase)
            bundle.putBoolean(REMOVE_ACCESS, removeAccess)
            val fragment = RecoveryPhraseShowingFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_recovery_showing

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

        val recoveryPhrase = arguments?.getStringArray(RECOVERY_PHRASE)!!
        val removeAccess = arguments?.getBoolean(REMOVE_ACCESS) ?: false

        if (removeAccess) {
            toolbarTitle.setText(R.string.write_down_recovery_phrase)
            btnTestRecoveryPhrase.gone()
            btnDone.setBackgroundDrawable(R.drawable.bg_blue_ribbon_stateful)
            btnDone.setTextColorRes(android.R.color.white)
        } else {
            toolbarTitle.setText(R.string.recovery_phrase)
            btnTestRecoveryPhrase.visible()
            btnDone.setBackgroundDrawable(R.drawable.bg_alice_blue_stateful)
            btnDone.setTextColorRes(R.color.blue_ribbon)
        }

        val adapter = RecoveryPhraseAdapter(
            editable = false,
            textColor = R.color.blue_ribbon
        )
        val layoutManager =
            GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)
        rvRecoveryPhrase.layoutManager = layoutManager
        rvRecoveryPhrase.isNestedScrollingEnabled = false
        rvRecoveryPhrase.adapter = adapter
        adapter.set(recoveryPhrase)

        btnDone.setSafetyOnclickListener {
            if (removeAccess) {
                navigateTestRecoveryPhrase(recoveryPhrase, true)
            } else {
                navigator.popChildFragmentToRoot()
            }
        }

        btnTestRecoveryPhrase.setSafetyOnclickListener {
            navigateTestRecoveryPhrase(recoveryPhrase, removeAccess)
        }

        ivBack.setOnClickListener { navigator.popChildFragmentToRoot() }

    }

    private fun navigateTestRecoveryPhrase(
        recoveryPhrase: Array<String>,
        removeAccess: Boolean
    ) {
        navigator.anim(
            RIGHT_LEFT
        ).replaceChildFragment(
            R.id.layoutContainer,
            RecoveryPhraseTestFragment.newInstance(
                recoveryPhrase,
                removeAccess
            )
        )
    }

    override fun onBackPressed() =
        navigator.popChildFragmentToRoot()
}