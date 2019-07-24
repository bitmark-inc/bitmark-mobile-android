package com.bitmark.registry.feature.property_detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.apiservice.utils.Address
import com.bitmark.apiservice.utils.error.HttpException
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.transfer.TransferFragment
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.registry.util.view.InfoAppCompatDialog
import com.bitmark.registry.util.view.ProgressAppCompatDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import kotlinx.android.synthetic.main.fragment_property_detail.*
import kotlinx.android.synthetic.main.layout_property_menu.view.*
import java.io.File
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertyDetailFragment : BaseSupportFragment() {

    companion object {

        private const val BITMARK = "bitmark"

        fun newInstance(bitmark: BitmarkModelView): PropertyDetailFragment {
            val bundle = Bundle()
            bundle.putParcelable(BITMARK, bitmark)
            val fragment = PropertyDetailFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private lateinit var bitmark: BitmarkModelView

    @Inject
    lateinit var viewModel: PropertyDetailViewModel

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var dialogController: DialogController

    private var blocked = false

    private val provenanceAdapter = ProvenanceRecyclerViewAdapter()

    private val handler = Handler()

    private lateinit var progressDialog: ProgressAppCompatDialog

    override fun layoutRes(): Int = R.layout.fragment_property_detail

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getProvenance(bitmark.id)
        viewModel.syncProvenance(bitmark.id)
    }

    override fun initComponents() {
        super.initComponents()

        bitmark = arguments?.getParcelable(BITMARK)!!
        val context = this.context!!

        showAssetType(bitmark.assetType)

        tvToolbarTitle.text =
            if (bitmark.name.isNullOrBlank()) getString(R.string.your_properties) else bitmark.name
        tvAssetName.text =
            if (bitmark.name.isNullOrBlank()) "" else bitmark.name
        tvIssuedOn.text =
            if (bitmark.isSettled()) getString(R.string.issued_on) + " " + bitmark.confirmedAt() else getString(
                R.string.pending
            ) + "...."

        // display with corresponding status
        val color = if (bitmark.isSettled()) ContextCompat.getColor(
            context,
            android.R.color.black
        ) else ContextCompat.getColor(context, R.color.silver)
        tvAssetName.setTextColor(color)
        tvIssuedOn.setTextColor(color)

        val rvMetadataLayoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val metadataAdapter =
            MetadataRecyclerViewAdapter(color)
        rvMetadata.layoutManager = rvMetadataLayoutManager
        rvMetadata.adapter = metadataAdapter
        metadataAdapter.set(bitmark.metadata ?: mapOf())

        val rvProvenanceLayoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rvProvenance.layoutManager = rvProvenanceLayoutManager
        rvProvenance.adapter = provenanceAdapter

        ivAction.setOnClickListener {
            ivAction.isSelected = true
            showPopupMenu(bitmark)
        }

        ivBack.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).finishActivity()
        }

    }

    override fun deinitComponents() {
        dialogController.dismiss()
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
    }

    private fun showAssetType(type: BitmarkModelView.AssetType) {
        ivAssetType.setImageResource(
            when (type) {
                BitmarkModelView.AssetType.IMAGE -> R.drawable.ic_asset_image
                BitmarkModelView.AssetType.VIDEO -> R.drawable.ic_asset_video
                BitmarkModelView.AssetType.HEALTH -> R.drawable.ic_asset_health_data
                BitmarkModelView.AssetType.MEDICAL -> R.drawable.ic_asset_medical_record
                BitmarkModelView.AssetType.ZIP -> R.drawable.ic_asset_zip
                BitmarkModelView.AssetType.DOC -> R.drawable.ic_asset_doc
                BitmarkModelView.AssetType.UNKNOWN -> R.drawable.ic_asset_unknow
            }
        )
    }

    private fun showPopupMenu(bitmark: BitmarkModelView) {
        val inflater =
            context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
                Handler().postDelayed({
                    tvSubItem1.invisible()
                    popupWindow.dismiss()
                }, 1000)
            }

            item2.setOnClickListener {
                popupWindow.dismiss()
                if (blocked) return@setOnClickListener
                if (downloadable) {
                    // download file
                    downloadAssetFile()
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
                navigator.anim(RIGHT_LEFT).replaceFragment(
                    R.id.layoutContainer,
                    TransferFragment.newInstance(bitmark)
                )
            }

            item4.setOnClickListener {
                // delete
                popupWindow.dismiss()
                if (blocked) return@setOnClickListener
                dialogController.confirm(
                    "",
                    getString(R.string.this_bitmark_will_be_deleted),
                    false,
                    getString(R.string.delete),
                    {
                        deleteBitmark()
                    },
                    getString(R.string.cancel)
                )
            }
        }

        popupWindow.showAsDropDown(ivAction)
    }

    override fun observe() {
        super.observe()
        viewModel.getProvenanceLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val p = res.data()
                    val txs = p?.second
                    if (txs == null || txs.isEmpty()) return@Observer
                    bitmark.previousOwner = txs[0].previousOwner
                    provenanceAdapter.set(p.first, txs)
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
                    val p = res.data()
                    val txs = p?.second
                    if (txs == null || txs.isEmpty()) return@Observer
                    bitmark.previousOwner = txs[0].previousOwner
                    provenanceAdapter.set(p.first, txs)
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
                        context!!,
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
                        getString(R.string.error),
                        res.throwable()?.message!!
                    )
                }

                res.isLoading() -> {
                    progressBar.visible()
                    blocked = true
                }
            }
        })

        viewModel.downloadAssetFileLiveData().observe(this, Observer { res ->

            when {
                res.isSuccess() -> {
                    dialogController.dismiss(progressDialog)
                    val file = res.data()
                    if (file != null) {
                        bitmark.assetFile = file
                        bitmark.assetType =
                            BitmarkModelView.determineAssetType(assetFile = bitmark.assetFile)
                        showAssetType(bitmark.assetType)
                        shareFile(bitmark.name ?: "", file)
                    }

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
                        context!!,
                        title = getString(R.string.preparing_to_export),
                        message = message
                    )
                    dialogController.show(progressDialog)
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
    }

    private fun deleteBitmark() {
        loadAccount(
            bitmark.accountNumber,
            getString(R.string.please_sign_to_delete_bitmark)
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

    private fun downloadAssetFile() {
        if (bitmark.previousOwner == null) return
        loadAccount(
            bitmark.accountNumber,
            getString(R.string.please_sign_to_download_asset)
        ) { account ->
            viewModel.downloadAssetFile(
                bitmark.assetId,
                bitmark.previousOwner!!,
                bitmark.accountNumber,
                account.encryptionKey
            )
        }
    }

    private fun shareFile(assetName: String, file: File) {
        val uri = FileProvider.getUriForFile(
            context!!,
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
        message: String,
        action: (Account) -> Unit
    ) {
        val spec =
            KeyAuthenticationSpec.Builder(context).setKeyAlias(accountNumber)
                .setAuthenticationDescription(message)
                .build()

        activity?.loadAccount(
            accountNumber,
            spec,
            dialogController,
            successAction = action,
            setupRequiredAction = { gotoSecuritySetting() },
            unknownErrorAction = {
                dialogController.alert(
                    R.string.error,
                    R.string.unexpected_error
                )
            })
    }

    private fun gotoSecuritySetting() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        navigator.anim(BOTTOM_UP).startActivity(intent)
    }
}