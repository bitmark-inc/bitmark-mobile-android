package com.bitmark.registry.feature

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.bitmark.registry.R
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.visible
import kotlinx.android.synthetic.main.layout_webview.*


/**
 * @author Hieu Pham
 * @since 2019-07-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class WebViewFragment : Fragment(), BehaviorComponent {

    companion object {

        private const val URL = "url"

        private const val TITLE = "title"

        fun newInstance(url: String, title: String? = null): WebViewFragment {
            val fragment = WebViewFragment()
            val bundle = Bundle()
            bundle.putString(URL, url)
            if (title != null) bundle.putString(TITLE, title)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var url: String? = null

    private var visibled = false

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

        webview.settings.javaScriptEnabled = true
        webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                visibled = true
                progressBar.gone()
            }

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: Bitmap?
            ) {
                super.onPageStarted(view, url, favicon)
                progressBar.visible()
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
        webview.webViewClient = null
    }

    override fun onResume() {
        super.onResume()
        if (!visibled) {
            load()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && !visibled) {
            load()
        }
    }

    private fun load() {
        // a bit delay for better performance
        handler.postDelayed({ webview.loadUrl(url) }, 200)
        visibled = true
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