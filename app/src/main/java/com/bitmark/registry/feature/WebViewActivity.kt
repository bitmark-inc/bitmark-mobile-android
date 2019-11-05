/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature

import android.os.Bundle
import android.os.Handler
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.bitmark.registry.R
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.visible
import kotlinx.android.synthetic.main.layout_webview.*

class WebViewActivity : AppCompatActivity() {

    companion object {

        private const val URL = "url"

        private const val TITLE = "title"

        private const val HAS_NAV = "has_nav"

        fun getBundle(
            url: String,
            title: String? = null,
            hasNav: Boolean = false
        ): Bundle {
            val bundle = Bundle()
            bundle.putString(URL, url)
            bundle.putBoolean(HAS_NAV, hasNav)
            if (title != null) bundle.putString(TITLE, title)
            return bundle
        }
    }

    private val navigator = Navigator(this)

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_webview)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        initComponents()
    }

    override fun onDestroy() {
        deinitComponents()
        super.onDestroy()
    }

    private fun initComponents() {
        val url = intent?.extras?.getString(URL)
        val title = intent?.extras?.getString(TITLE)
        if (title != null) {
            tvToolbarTitle.text = title
            layoutToolbar.visible()
        } else {
            layoutToolbar.gone()
        }

        val hasNav = intent?.extras?.getBoolean(HAS_NAV) ?: false
        if (hasNav) {
            layoutNav.visible()
        } else {
            layoutNav.gone()
        }

        webview.settings.javaScriptEnabled = true

        webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.gone()
                } else {
                    progressBar.visible()
                }
            }
        }

        ivBack.setOnClickListener { webview.goBack() }

        ivNext.setOnClickListener { webview.goForward() }

        ivToolbarBack.setOnClickListener {
            navigator.anim(RIGHT_LEFT).finishActivity()
        }

        // a bit delay for better performance
        handler.postDelayed({ webview.loadUrl(url) }, 200)
    }

    private fun deinitComponents() {
        handler.removeCallbacksAndMessages(null)
        webview.webChromeClient = null
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }
}