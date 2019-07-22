package com.bitmark.registry.feature

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.bitmark.registry.R
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.visible
import kotlinx.android.synthetic.main.layout_webview.*


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class WebViewActivity : AppCompatActivity() {

    companion object {

        private const val URL = "url"

        private const val TITLE = "title"

        fun getBundle(url: String, title: String? = null): Bundle {
            val bundle = Bundle()
            bundle.putString(URL, url)
            if (title != null) bundle.putString(TITLE, title)
            return bundle
        }
    }

    private val navigator = Navigator(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_webview)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        initComponents()
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

        webview.settings.javaScriptEnabled = true
        webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
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
            navigator.anim(RIGHT_LEFT).finishActivity()
        }

        // a bit delay for better performance
        Handler().postDelayed({ webview.loadUrl(url) }, 200)
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }
}