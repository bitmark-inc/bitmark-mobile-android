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
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import kotlinx.android.synthetic.main.fragment_recovery_phrase_signin.rvRecoveryPhrase
import kotlinx.android.synthetic.main.fragment_recovery_showing.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-24
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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

        toolbarTitle.setText(if (removeAccess) R.string.recovery_phrase_sign_out else R.string.recovery_phrase)

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

        btnDone.setSafetyOnclickListener { navigator.popChildFragmentToRoot() }

        btnTestRecoveryPhrase.setSafetyOnclickListener {
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

        ivBack.setOnClickListener { navigator.popChildFragmentToRoot() }

    }

    override fun onBackPressed() =
        navigator.popChildFragmentToRoot()
}