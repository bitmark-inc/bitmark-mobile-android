package com.bitmark.registry.feature.account.settings

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.account.settings.details.SettingsDetailsFragment
import com.bitmark.registry.feature.recoveryphrase.show.RecoveryPhraseWarningFragment
import com.bitmark.registry.util.extension.copyToClipboard
import com.bitmark.registry.util.extension.invisible
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.visible
import com.bitmark.registry.util.view.QrCodeSharingDialog
import io.intercom.android.sdk.Intercom
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SettingsFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    @Inject
    lateinit var viewModel: SettingsViewModel

    @Inject
    lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_settings

    override fun viewModel(): BaseViewModel? = viewModel

    private var accountNumber: String = ""

    private val handler = Handler()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getAccountNumber()
    }

    override fun initComponents() {
        super.initComponents()

        tvAccountNumber.setOnClickListener {
            context?.copyToClipboard(accountNumber)
            tvCopyClipboard.visible()
            handler.postDelayed({ tvCopyClipboard.invisible() }, 1000)
        }

        tvWriteDownPhrase.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).replaceChildFragment(
                R.id.layoutContainer,
                RecoveryPhraseWarningFragment.newInstance(
                    getString(R.string.recovery_phrase),
                    getString(R.string.your_recovery_phrase_is_the_only)
                )
            )
        }

        tvLogout.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).replaceChildFragment(
                R.id.layoutContainer,
                RecoveryPhraseWarningFragment.newInstance(
                    getString(R.string.remove_access),
                    getString(R.string.your_recovery_phrase_is_the_only_way_to_access),
                    true
                )
            )
        }

        tvDetail.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).replaceChildFragment(
                R.id.layoutContainer,
                SettingsDetailsFragment.newInstance()
            )
        }

        tvNeedHelp.setSafetyOnclickListener {
            Intercom.client().displayMessenger()
        }

        ivQrCode.setSafetyOnclickListener {
            if (accountNumber.isEmpty()) return@setSafetyOnclickListener
            val dialog = QrCodeSharingDialog(context!!, accountNumber)
            dialog.ownerActivity = activity
            dialog.show()
        }
    }

    override fun deinitComponents() {
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()
        viewModel.getAccountNumberLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    accountNumber = res.data()!!
                    tvAccountNumber.text = accountNumber
                }
            }
        })
    }
}