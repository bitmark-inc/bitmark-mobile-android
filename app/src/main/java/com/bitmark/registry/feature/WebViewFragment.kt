/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.bitmark.registry.R
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.visible
import kotlinx.android.synthetic.main.layout_webview.*

class WebViewFragment : Fragment(), BehaviorComponent {

    companion object {

        private const val URL = "url"

        private const val TITLE = "title"

        private const val PRELOAD = "preload"

        private const val HAS_NAV = "has_nav"

        fun newInstance(
            url: String,
            title: String? = null,
            preload: Boolean = false,
            hasNav: Boolean = false
        ): WebViewFragment {
            val fragment = WebViewFragment()
            val bundle = Bundle()
            bundle.putString(URL, url)
            bundle.putBoolean(PRELOAD, preload)
            bundle.putBoolean(HAS_NAV, hasNav)
            if (title != null) bundle.putString(TITLE, title)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var url: String? = null

    private var preload = false

    private var loaded = false

    private val handler = Handler()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initComponents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deinitComponents()
    }

    private fun initComponents() {
        url = arguments?.getString(URL)
        val title = arguments?.getString(TITLE)

        if (title != null) {
            tvToolbarTitle.text = title
            layoutToolbar.visible()
        } else {
            layoutToolbar.gone()
        }

        val hasNav = arguments?.getBoolean(HAS_NAV) ?: false
        if (hasNav) {
            layoutNav.visible()
        } else {
            layoutNav.gone()
        }

        preload = arguments?.getBoolean(PRELOAD) ?: false

        webview.settings.javaScriptEnabled = true
        webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.gone()
                    loaded = true
                } else {
                    progressBar.visible()
                }
            }
        }

        ivBack.setOnClickListener { webview.goBack() }

        ivNext.setOnClickListener { webview.goForward() }

        ivToolbarBack.setOnClickListener {
            destroy()
        }
    }

    private fun deinitComponents() {
        handler.removeCallbacksAndMessages(null)
        webview.webChromeClient = null
        loaded = false
    }

    override fun onResume() {
        super.onResume()
        if (preload && !loaded) {
            load()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && !loaded) {
            load()
        }
    }

    private fun load() {
        // a bit delay for better performance
        handler.postDelayed({ webview.loadUrl(url) }, 200)
    }

    override fun onBackPressed(): Boolean = destroy()

    private fun destroy(): Boolean {
        val navigator =
            if (parentFragment != null) Navigator(parentFragment!!) else Navigator(
                this
            )

        var onBackPressed = navigator.anim(RIGHT_LEFT).popFragment() ?: false
        if (!onBackPressed) {
            onBackPressed =
                navigator.anim(RIGHT_LEFT).popChildFragment() ?: false
        }
        return onBackPressed
    }
}