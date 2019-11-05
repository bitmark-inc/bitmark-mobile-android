/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.account

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.account.details.SettingsDetailsFragment
import com.bitmark.registry.feature.cloud_service_sign_in.CloudServiceSignInActivity
import com.bitmark.registry.feature.recoveryphrase.show.RecoveryPhraseWarningFragment
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.view.QrCodeSharingDialog
import io.intercom.android.sdk.Intercom
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject

class SettingsFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() =
            SettingsFragment()
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
        viewModel.checkCloudServiceRequired()
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

        tvBackupToDrive.setSafetyOnclickListener {
            navigator.anim(BOTTOM_UP)
                .startActivity(CloudServiceSignInActivity::class.java)
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
            openIntercom()
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

        viewModel.checkCloudServiceRequiredLiveData.observe(
            this,
            Observer { required ->
                setCloudServiceVisibility(required)
            })

        viewModel.cloudServiceRequiredChangedLiveData.observe(
            this,
            Observer { required ->
                setCloudServiceVisibility(required)
            })
    }

    private fun setCloudServiceVisibility(visible: Boolean) {
        if (visible) {
            tvBackupToDrive.visible()
        } else {
            tvBackupToDrive.gone()
        }
    }

    fun openIntercom() {
        Intercom.client().displayMessenger()
    }
}