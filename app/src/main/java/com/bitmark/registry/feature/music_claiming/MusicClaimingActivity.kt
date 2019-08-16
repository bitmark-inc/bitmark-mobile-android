package com.bitmark.registry.feature.music_claiming

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.bitmark.apiservice.utils.error.HttpException
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.feature.*
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.transfer.TransferActivity
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.AssetClaimingModelView
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.registry.util.view.OptionsDialog
import com.bitmark.registry.util.view.ProgressAppCompatDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_music_claiming.*
import kotlinx.android.synthetic.main.layout_bitmark_cert.*
import java.io.File
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-08-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MusicClaimingActivity : BaseAppCompatActivity() {

    companion object {
        private const val BITMARK = "bitmark"

        fun getBundle(
            bitmark: BitmarkModelView
        ): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(BITMARK, bitmark)
            return bundle
        }
    }

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var viewModel: MusicClaimingViewModel

    @Inject
    lateinit var dialogController: DialogController

    private lateinit var progressDialog: ProgressAppCompatDialog

    private lateinit var bitmark: BitmarkModelView

    private lateinit var assetClaiming: AssetClaimingModelView

    override fun layoutRes(): Int = R.layout.activity_music_claiming

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getMusicClaimingInfo(
            bitmark.assetId,
            bitmark.id,
            bitmark.edition
        )
    }

    override fun requestFeatures() {
        super.requestFeatures()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    override fun initComponents() {
        super.initComponents()

        bitmark = intent?.extras?.getParcelable(BITMARK) as BitmarkModelView
        tvAssetId.text = bitmark.assetId
        tvIssuanceDate.text = bitmark.createdAt()

        val bmCertBehavior = BottomSheetBehavior.from(layoutRootCert)
        setBmCertVisibility(bmCertBehavior, false)

        wvContent.settings.javaScriptEnabled = true
        wvContent.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.gone()
                    tvLoading.gone()
                } else {
                    progressBar.visible()
                    tvLoading.visible()
                }
            }
        }

        wvContent.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && request?.isRedirect == true) {
                    view?.loadUrl(request.url.toString())
                    return false
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                url: String?
            ): Boolean {
                view?.loadUrl(url)
                return false
            }
        }

        wvContent.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val height =
                Math.floor((wvContent.contentHeight * wvContent.scale).toDouble())
                    .toInt()
            val webViewHeight = wvContent.measuredHeight
            if (wvContent.scrollY + webViewHeight >= height - 300 && oldScrollY < scrollY) {
                setBmCertVisibility(bmCertBehavior, true)
            } else {
                setBmCertVisibility(bmCertBehavior, false)
            }
        }

        ivClose.setOnClickListener {
            navigator.anim(BOTTOM_UP).finishActivity()
        }

        btnViewBmOpt.setSafetyOnclickListener {
            showOptionsDialog(bitmark)
        }

        layoutRootCert.setOnClickListener {
            val url =
                "${BuildConfig.REGISTRY_WEBSITE}/bitmark/${bitmark.id}?env=app"
            navigator.anim(RIGHT_LEFT).startActivity(
                WebViewActivity::class.java,
                WebViewActivity.getBundle(url, getString(R.string.registry))
            )
        }

        setBtnViewBmOptsEnable(bitmark.isSettled())
    }

    private fun setBtnViewBmOptsEnable(enable: Boolean) {
        if (enable) {
            btnViewBmOpt.setText(R.string.view_bitmark_opt)
            btnViewBmOpt.enable()
        } else {
            btnViewBmOpt.setText(R.string.authenticating_transfer_to_you_three_dot)
            btnViewBmOpt.disable()
        }
    }

    override fun deinitComponents() {
        wvContent.setOnScrollChangeListener(null)
        wvContent.webViewClient = null
        wvContent.webChromeClient = null
        wvContent.reload()
        wvContent.destroy()
        super.deinitComponents()
    }

    private fun setBmCertVisibility(
        behavior: BottomSheetBehavior<*>,
        visible: Boolean
    ) {
        if (visible) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun showOptionsDialog(bitmark: BitmarkModelView) {
        val opts = listOf(
            OptionsDialog.OptionsAdapter.Item(
                R.drawable.ic_play_circle_filled,
                getString(R.string.play_on_streaming_platform)
            ),
            OptionsDialog.OptionsAdapter.Item(
                R.drawable.ic_download,
                getString(R.string.download_property)
            ),
            OptionsDialog.OptionsAdapter.Item(
                R.drawable.ic_swap,
                getString(R.string.transfer_ownership)
            )
        )
        val optionsDialog = OptionsDialog(
            this,
            getString(R.string.bitmark_option),
            opts
        ) { item ->
            when (item.icon) {
                R.drawable.ic_play_circle_filled -> {
                    val url = bitmark.metadata?.get("playlink")
                        ?: return@OptionsDialog
                    navigator.openBrowser(url)
                }

                R.drawable.ic_download -> {
                    if (bitmark.previousOwner == null) return@OptionsDialog
                    if (bitmark.assetFile != null) {
                        dialogController.alert(
                            "",
                            getString(R.string.you_have_already_downloaded_this_asset)
                        )
                    } else {
                        viewModel.prepareDownload()
                    }
                }

                R.drawable.ic_swap -> {
                    if (bitmark.isPending()) return@OptionsDialog
                    if (bitmark.assetFile == null) {
                        dialogController.confirm(
                            R.string.warning,
                            R.string.to_be_able_to_transfer_this_bitmark,
                            false,
                            R.string.download,
                            { if (bitmark.previousOwner != null) viewModel.prepareDownload() },
                            R.string.cancel
                        )
                    } else {
                        navigator.anim(RIGHT_LEFT).startActivity(
                            TransferActivity::class.java,
                            TransferActivity.getBundle(bitmark)
                        )
                    }
                }
            }
        }

        optionsDialog.show()
    }

    override fun observe() {
        super.observe()

        viewModel.getMusicClaimingInfoLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val data = res.data() ?: return@Observer
                    assetClaiming = data.first
                    if (data.second.isNotEmpty()) bitmark.previousOwner =
                        data.second
                    val url =
                        "${BuildConfig.MOBILE_SERVER_ENDPOINT}/api/claim_requests_view/${assetClaiming.assetId}?total=${assetClaiming.limitedEdition}&remaining=${assetClaiming.totalEditionLeft}&edition_number=${assetClaiming.editionNumber
                            ?: "?"}"
                    wvContent.loadUrl(url)
                }

                res.isError() -> {
                    dialogController.alert(
                        getString(R.string.error),
                        res.throwable()?.message
                            ?: getString(R.string.unexpected_error)
                    ) { navigator.anim(BOTTOM_UP).finishActivity() }
                }
            }
        })

        viewModel.prepareDownloadLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val data = res.data() ?: return@Observer
                    val accountNumber = data.first
                    val keyAlias = data.second
                    loadAccount(accountNumber, keyAlias) { account ->
                        viewModel.downloadAssetFile(
                            bitmark.assetId,
                            bitmark.previousOwner!!,
                            bitmark.accountNumber,
                            account.encryptionKey
                        )
                    }
                }
            }
        })

        viewModel.downloadProgressLiveData.observe(this, Observer { progress ->
            progressDialog.setProgress(progress)
        })

        viewModel.downloadAssetLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    dialogController.dismiss(progressDialog)
                    val file = res.data() ?: return@Observer
                    bitmark.assetFile = file
                    shareFile(bitmark.name ?: "", file)
                }

                res.isError() -> {
                    dialogController.dismiss(progressDialog)
                    val e = res.throwable()
                    val errorMessage =
                        if (e is HttpException && e.statusCode == 404) {
                            R.string.the_asset_is_not_available
                        } else {
                            R.string.could_not_download_asset
                        }
                    dialogController.alert(
                        R.string.error,
                        errorMessage,
                        R.string.ok
                    )
                }

                res.isLoading() -> {
                    val message = "%s \"%s\"...".format(
                        getString(R.string.downloading),
                        bitmark.name ?: ""
                    )
                    progressDialog = ProgressAppCompatDialog(
                        this,
                        getString(R.string.preparing_to_export),
                        message
                    )
                    dialogController.show(progressDialog)
                }
            }
        })

        viewModel.bitmarksSavedLiveData.observe(this, Observer { bitmarks ->
            val index = bitmarks.indexOfFirst { b -> b.id == bitmark.id }
            if (index == -1) return@Observer
            bitmark.status = bitmarks[index].status
            setBtnViewBmOptsEnable(bitmark.isSettled())
        })

        viewModel.bitmarkStatusChangedLiveData.observe(this, Observer { t ->
            val bitmarkId = t.first
            if (bitmarkId != bitmark.id) return@Observer
            if (t.third == BitmarkData.Status.TO_BE_TRANSFERRED) {
                navigator.anim(BOTTOM_UP).finishActivity()
            }
        })
    }

    private fun loadAccount(
        accountNumber: String,
        keyAlias: String,
        action: (Account) -> Unit
    ) {
        val spec = KeyAuthenticationSpec.Builder(this).setKeyAlias(keyAlias)
            .setAuthenticationDescription(getString(R.string.please_sign_to_download_asset))
            .build()
        this.loadAccount(accountNumber,
            spec,
            dialogController,
            successAction = action,
            setupRequiredAction = { navigator.gotoSecuritySetting() })
    }

    private fun shareFile(assetName: String, file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".file_provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_SUBJECT, assetName)
        intent.putExtra(Intent.EXTRA_TEXT, assetName)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        navigator.anim(BOTTOM_UP)
            .startActivity(Intent.createChooser(intent, assetName))
    }

    override fun onBackPressed() {
        navigator.anim(BOTTOM_UP).finishActivity()
        super.onBackPressed()
    }
}