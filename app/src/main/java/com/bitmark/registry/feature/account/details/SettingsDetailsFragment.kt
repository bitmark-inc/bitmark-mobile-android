package com.bitmark.registry.feature.account.details

import android.content.Intent
import android.net.Uri
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.WebViewFragment
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import kotlinx.android.synthetic.main.fragment_settings_details.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-29
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SettingsDetailsFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() =
            SettingsDetailsFragment()
    }

    @Inject
    lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_settings_details

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

        tvAppInfo.text = "%s: %s (%d)".format(
            getString(R.string.version),
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        tvTermsOfService.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).replaceChildFragment(
                R.id.layoutContainer,
                WebViewFragment.newInstance(
                    BuildConfig.TERMS_OF_SERVICE_URL,
                    getString(R.string.terms_of_service),
                    true
                )
            )
        }

        tvPrivacyPolicy.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).replaceChildFragment(
                R.id.layoutContainer,
                WebViewFragment.newInstance(
                    BuildConfig.PRIVACY_POLICY_URL,
                    getString(R.string.privacy_policy),
                    true
                )
            )
        }

        tvWhatNew.setSafetyOnclickListener {
            navigator.anim(BOTTOM_UP)
                .startActivity(WhatsNewActivity::class.java)
        }

        tvRating.setSafetyOnclickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)
            )
            navigator.startActivity(intent)
        }

        tvShareApp.setSafetyOnclickListener {
            val url = "https://play.google.com/store/apps/details?id=%s".format(
                BuildConfig.APPLICATION_ID
            )
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, url)
            navigator.anim(BOTTOM_UP)
                .startActivity(
                    Intent.createChooser(
                        intent,
                        getString(R.string.share_with)
                    )
                )
        }

        ivBack.setOnClickListener { navigator.popChildFragment() }
    }

    override fun onBackPressed(): Boolean {
        return navigator.popChildFragment() ?: false
    }
}