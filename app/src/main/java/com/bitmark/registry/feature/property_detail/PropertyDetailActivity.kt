package com.bitmark.registry.feature.property_detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.apiservice.utils.Address
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.source.remote.api.error.HttpException
import com.bitmark.registry.feature.*
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.transfer.TransferActivity
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.registry.util.view.InfoAppCompatDialog
import com.bitmark.registry.util.view.OptionsDialog
import com.bitmark.registry.util.view.ProgressAppCompatDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_property_detail.*
import kotlinx.android.synthetic.main.layout_property_menu.view.*
import java.io.File
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertyDetailActivity : BaseAppCompatActivity() {

    companion object {

        private const val BITMARK = "bitmark"

        fun getBundle(bitmark: BitmarkModelView): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(BITMARK, bitmark)
            return bundle
        }
    }

    private lateinit var bitmark: BitmarkModelView

    private lateinit var keyAlias: String

    @Inject
    lateinit var viewModel: PropertyDetailViewModel

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var dialogController: DialogController

    private var blocked = false

    private val provenanceAdapter = ProvenanceRecyclerViewAdapter()

    private val handler = Handler()

    private var progressDialog: ProgressAppCompatDialog? = null

    override fun layoutRes(): Int = R.layout.activity_property_detail

    override fun viewModel(): BaseViewModel? = viewModel

    private val metadataAdapter = MetadataRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getKeyAlias()
        viewModel.getProvenance(bitmark.id)
        viewModel.syncProvenance(bitmark.id)
    }

    override fun initComponents() {
        super.initComponents()

        bitmark = intent?.extras?.getParcelable(BITMARK) as BitmarkModelView
        viewModel.setBitmarkId(bitmark.id)

        ivAssetType.setImageResource(bitmark.getThumbnailRes())

        tvToolbarTitle.text =
            if (bitmark.name.isNullOrBlank()) getString(R.string.your_properties) else bitmark.name
        tvAssetName.text =
            if (bitmark.name.isNullOrBlank()) "" else bitmark.name
        tvIssuedOn.text =
            if (bitmark.isSettled()) getString(R.string.issued_on) + " " + bitmark.createdAt() else getString(
                R.string.pending
            ) + "...."

        // display with corresponding status
        val color =
            if (bitmark.isSettled()) android.R.color.black else R.color.silver
        tvAssetName.setTextColorRes(color)
        tvIssuedOn.setTextColorRes(color)

        val rvMetadataLayoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        metadataAdapter.changeTextColor(color)
        rvMetadata.layoutManager = rvMetadataLayoutManager
        rvMetadata.adapter = metadataAdapter
        metadataAdapter.set(bitmark.metadata ?: mapOf())

        val rvProvenanceLayoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rvProvenance.layoutManager = rvProvenanceLayoutManager
        rvProvenance.adapter = provenanceAdapter

        ivAction.setOnClickListener {
            ivAction.isSelected = true
            showPopupMenu(bitmark)
        }

        ivBack.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).finishActivity()
        }

        provenanceAdapter.setItemClickListener { item ->
            if (item.isPending) return@setItemClickListener
            val url = "%s/account/%s/owned?env=app".format(
                BuildConfig.REGISTRY_WEBSITE,
                item.owner
            )
            navigator.anim(RIGHT_LEFT).startActivity(
                WebViewActivity::class.java,
                WebViewActivity.getBundle(url, getString(R.string.registry))
            )
        }

    }

    override fun deinitComponents() {
        dialogController.dismiss()
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
    }

    private fun showPopupMenu(bitmark: BitmarkModelView) {
        val inflater =
            this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_property_menu, null)
        val popupWindow = PopupWindow(
            view,
            resources.getDimensionPixelSize(R.dimen.dp_200),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow.isFocusable = true
        popupWindow.setOnDismissListener { ivAction.isSelected = false }

        with(view) {

            val isSettled = bitmark.isSettled()
            val color = if (isSettled) ContextCompat.getColor(
                context,
                R.color.blue_ribbon
            ) else ContextCompat.getColor(context, R.color.silver)
            tvItem2.setTextColor(color)
            tvItem3.setTextColor(color)
            tvItem4.setTextColor(color)

            if (isSettled) {
                item2.enable()
                item3.enable()
                item4.enable()
            } else {
                item2.disable()
                item3.disable()
                item4.disable()
            }

            val downloadable =
                bitmark.assetFile == null && isSettled

            if (downloadable) {
                tvItem2.text = getString(R.string.download)
            } else {
                tvItem2.text = getString(R.string.share)
            }

            item1.setOnClickListener {
                // copy bitmark
                tvSubItem1.visible()
                context.copyToClipboard(bitmark.id)
                handler.postDelayed({
                    tvSubItem1.invisible()
                    popupWindow.dismiss()
                }, 1000)
            }

            item2.setOnClickListener {
                popupWindow.dismiss()
                if (blocked) return@setOnClickListener
                if (downloadable) {
                    // download file
                    downloadAssetFile(bitmark, keyAlias)
                } else {
                    // share bitmark
                    viewModel.getExistingAsset(
                        bitmark.accountNumber,
                        bitmark.assetId
                    )
                }
            }

            item3.setOnClickListener {
                // transfer
                popupWindow.dismiss()
                if (blocked) return@setOnClickListener
                if (downloadable) {
                    dialogController.confirm(
                        R.string.warning,
                        R.string.to_be_able_to_transfer_this_bitmark,
                        false,
                        R.string.download,
                        { downloadAssetFile(bitmark, keyAlias) },
                        R.string.cancel
                    )
                } else {
                    navigator.anim(RIGHT_LEFT).startActivity(
                        TransferActivity::class.java,
                        TransferActivity.getBundle(bitmark)
                    )
                }
            }

            item4.setOnClickListener {
                // delete
                popupWindow.dismiss()
                if (blocked) return@setOnClickListener

                val opts = listOf(
                    OptionsDialog.OptionsAdapter.Item(
                        R.drawable.ic_delete_2,
                        getString(R.string.delete),
                        R.color.torch_red
                    ),
                    OptionsDialog.OptionsAdapter.Item(
                        R.drawable.ic_cancel_2,
                        getString(R.string.cancel)
                    )
                )
                val optDialog = OptionsDialog(
                    context,
                    getString(R.string.this_bitmark_will_be_deleted),
                    false,
                    opts
                ) { item ->
                    if (item.icon == R.drawable.ic_delete_2) {
                        deleteBitmark(bitmark, keyAlias)
                    }
                }

                optDialog.show()
            }
        }

        popupWindow.showAsDropDown(ivAction)
    }

    override fun observe() {
        super.observe()
        viewModel.getProvenanceLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val txs = res.data()
                    if (txs == null || txs.isEmpty()) return@Observer
                    bitmark.previousOwner = txs[0].previousOwner
                    provenanceAdapter.set(txs)
                }
            }
        })

        viewModel.syncProvenanceLiveData().observe(this, Observer { res ->
            when {
                res.isLoading() -> {
                    progressBar.visible()
                }

                res.isSuccess() -> {
                    progressBar.gone()
                    val txs = res.data()
                    if (txs == null || txs.isEmpty()) return@Observer
                    bitmark.previousOwner = txs[0].previousOwner
                    provenanceAdapter.set(txs)
                }

                res.isError() -> {
                    progressBar.gone()
                }
            }
        })

        viewModel.deleteBitmarkLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    progressBar.gone()
                    blocked = false
                    val dialog = InfoAppCompatDialog(
                        this,
                        getString(R.string.your_property_rights_has_been_deleted)
                    )
                    dialogController.show(dialog)

                    handler.postDelayed({
                        dialogController.dismiss(dialog) {
                            navigator.anim(
                                RIGHT_LEFT
                            ).finishActivity()
                        }
                    }, 1500)
                }

                res.isError() -> {
                    progressBar.gone()
                    blocked = false
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_delete_bitmark
                    )
                }

                res.isLoading() -> {
                    progressBar.visible()
                    val snackbar = Snackbar.make(
                        rvMetadata,
                        R.string.deleting_your_rights_three_dot,
                        Snackbar.LENGTH_SHORT
                    )
                    val view = snackbar.view
                    view.background =
                        getDrawable(R.drawable.bg_wild_sand_shadow)
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        ?.setTextColorRes(android.R.color.black)
                    snackbar.show()
                    blocked = true
                }
            }
        })

        viewModel.downloadAssetFileLiveData().observe(this, Observer { res ->

            when {
                res.isSuccess() -> {
                    val file = res.data()
                    if (file != null) {
                        bitmark.assetFile = file
                        ivAssetType.setImageResource(bitmark.getThumbnailRes())
                        shareFile(bitmark.name ?: "", file)
                    }

                }

                res.isError() -> {
                    dialogController.dismiss(progressDialog ?: return@Observer)
                    val e = res.throwable()
                    val errorMessage =
                        if (e is HttpException && e.code == 404) {
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
                        title = getString(R.string.preparing_to_export),
                        message = message
                    )
                    dialogController.show(progressDialog ?: return@Observer)
                }
            }
        })

        viewModel.getExistingAssetFileLiveData().observe(this, Observer { res ->
            if (res.isSuccess()) {
                val file = res.data()
                if (file != null) {
                    shareFile(bitmark.name ?: "", file)
                }
            }
        })

        viewModel.getKeyAliasLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    keyAlias = res.data()!!
                }

                res.isError() -> {
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error,
                        R.string.ok
                    ) { navigator.finishActivity() }
                }
            }
        })

        viewModel.downloadProgressLiveData.observe(this, Observer { percent ->
            progressDialog?.setProgress(percent)
            if (percent >= 100) {
                dialogController.dismiss(progressDialog ?: return@Observer)
            }
        })

        viewModel.bitmarkSavedLiveData.observe(this, Observer { bitmark ->
            this.bitmark = bitmark
            val color =
                if (bitmark.isSettled()) android.R.color.black else R.color.silver
            tvAssetName.setTextColorRes(color)
            tvIssuedOn.setTextColorRes(color)
            tvIssuedOn.text =
                if (bitmark.isSettled()) getString(R.string.issued_on) + " " + bitmark.createdAt() else getString(
                    R.string.pending
                ) + "...."
            metadataAdapter.changeTextColor(color)
        })

        viewModel.txsSavedLiveData.observe(this, Observer { txs ->
            if (txs.isEmpty()) return@Observer
            bitmark.previousOwner = txs[0].previousOwner
            provenanceAdapter.set(txs)
        })

        viewModel.bitmarkDeletedLiveData.observe(this, Observer { p ->
            when (p.second) {

                BitmarkData.Status.TO_BE_DELETED -> {
                    // do nothing since it's already handled
                }

                else -> {
                    navigator.anim(RIGHT_LEFT).finishActivity()
                }
            }
        })

        viewModel.assetFileSavedLiveData.observe(this, Observer { p ->
            val assetId = p.first
            if (bitmark.assetId != assetId) return@Observer
            bitmark.assetFile = p.second
            ivAssetType.setImageResource(bitmark.getThumbnailRes())
        })
    }

    private fun deleteBitmark(bitmark: BitmarkModelView, keyAlias: String) {
        loadAccount(
            bitmark.accountNumber,
            keyAlias
        ) { account ->
            val zeroAddr = Address.fromAccountNumber(BuildConfig.ZERO_ADDRESS)
            val transferParams = TransferParams(zeroAddr, bitmark.headId)
            transferParams.sign(account.keyPair)
            viewModel.deleteBitmark(
                transferParams,
                bitmark.id,
                bitmark.assetId
            )
        }
    }

    private fun downloadAssetFile(bitmark: BitmarkModelView, keyAlias: String) {
        if (bitmark.previousOwner == null) return
        loadAccount(
            bitmark.accountNumber,
            keyAlias
        ) { account ->
            viewModel.downloadAssetFile(
                bitmark.assetId,
                account.accountNumber,
                account.encryptionKey
            )
        }
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

    private fun loadAccount(
        accountNumber: String,
        keyAlias: String,
        action: (Account) -> Unit
    ) {
        val spec =
            KeyAuthenticationSpec.Builder(this).setKeyAlias(keyAlias)
                .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
                .setUsePossibleAlternativeAuthentication(true)
                .build()

        loadAccount(
            accountNumber,
            spec,
            dialogController,
            successAction = action,
            setupRequiredAction = { navigator.gotoSecuritySetting() },
            unknownErrorAction = {
                dialogController.alert(
                    R.string.error,
                    R.string.unexpected_error
                )
            })
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }
}