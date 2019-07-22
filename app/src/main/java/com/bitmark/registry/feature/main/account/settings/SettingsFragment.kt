package com.bitmark.registry.feature.main.account.settings

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.extension.copyToClipboard
import com.bitmark.registry.util.extension.invisible
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.visible
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

        tvWriteDownPhrase.setSafetyOnclickListener { }

        tvLogout.setSafetyOnclickListener { }

        tvDetail.setSafetyOnclickListener { }

        tvNeedHelp.setSafetyOnclickListener { }
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