package com.bitmark.registry.feature.property_detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.apiservice.utils.Address
import com.bitmark.apiservice.utils.callback.Callback1
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.authentication.error.AuthenticationException
import com.bitmark.sdk.authentication.error.AuthenticationRequiredException
import com.bitmark.sdk.features.Account
import kotlinx.android.synthetic.main.activity_property_detail.*
import kotlinx.android.synthetic.main.layout_property_menu.view.*
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

    private var bitmark: BitmarkModelView? = null

    @Inject
    lateinit var viewModel: PropertyDetailViewModel

    @Inject
    lateinit var navigator: Navigator<PropertyDetailActivity>

    @Inject
    lateinit var dialogController: DialogController

    private var blocked = false

    private val provenanceAdapter = ProvenanceRecyclerViewAdapter()

    override fun layoutRes(): Int = R.layout.activity_property_detail

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        viewModel.getProvenance(bitmark!!.id)
        viewModel.syncProvenance(bitmark!!.id)
    }

    override fun initComponents() {
        super.initComponents()

        val bundle = intent.extras
        bitmark = bundle?.getParcelable(BITMARK)!!

        ivAssetType.setImageResource(
            when (bitmark?.assetType) {
                BitmarkModelView.AssetType.IMAGE -> R.drawable.ic_asset_image
                BitmarkModelView.AssetType.VIDEO -> R.drawable.ic_asset_video
                BitmarkModelView.AssetType.HEALTH -> R.drawable.ic_asset_health_data
                BitmarkModelView.AssetType.MEDICAL -> R.drawable.ic_asset_medical_record
                BitmarkModelView.AssetType.ZIP -> R.drawable.ic_asset_zip
                BitmarkModelView.AssetType.DOC -> R.drawable.ic_asset_doc
                BitmarkModelView.AssetType.UNKNOWN -> R.drawable.ic_asset_unknow
                else -> R.drawable.ic_asset_unknow
            }
        )

        tvAssetName.text = bitmark?.name
        tvIssuedOn.text =
            if (bitmark?.isSettled() != false) getString(R.string.issued_on) + " " + bitmark?.confirmedAt() else getString(
                R.string.pending
            ) + "...."

        val rvMetadataLayoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val metadataAdapter = MetadataRecyclerViewAdapter()
        rvMetadata.layoutManager = rvMetadataLayoutManager
        rvMetadata.adapter = metadataAdapter
        metadataAdapter.add(bitmark?.metadata ?: mapOf())

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

    }

    private fun showPopupMenu(bitmark: BitmarkModelView?) {
        val inflater =
            applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_property_menu, null)
        val popupWindow = PopupWindow(
            view,
            resources.getDimensionPixelSize(R.dimen.dp_200),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow.isFocusable = true
        popupWindow.setOnDismissListener { ivAction.isSelected = false }

        with(view) {

            if (bitmark?.isSettled() != false) {
                item3.enable()
                item4.enable()
                tvItem3.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.blue_ribbon
                    )
                )
                tvItem4.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.blue_ribbon
                    )
                )
            } else {
                item3.disable()
                item4.disable()
                tvItem3.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.silver
                    )
                )
                tvItem4.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.silver
                    )
                )
            }

            if (bitmark?.assetFile != null || !bitmark?.isSettled()!!) {
                tvItem1.text = getString(R.string.share)
            } else {
                tvItem1.text = getString(R.string.download)
            }

            item1.setOnClickListener {
                // download or share
                popupWindow.dismiss()
                if (blocked) return@setOnClickListener
            }
            item2.setOnClickListener {
                // copy bitmark
                tvSubItem2.visible()
                copyToClipboard(bitmark.id)
                Handler().postDelayed({
                    tvSubItem2.invisible()
                    popupWindow.dismiss()
                }, 1000)
            }
            item3.setOnClickListener {
                // transfer
                popupWindow.dismiss()
                if (blocked) return@setOnClickListener
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
                    if (p == null || p.second.isEmpty()) return@Observer
                    provenanceAdapter.set(p.first, p.second)
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
                    val p = res.data() ?: return@Observer
                    provenanceAdapter.set(p.first, p.second)
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
                    navigator.anim(RIGHT_LEFT).finishActivity()
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
    }

    private fun deleteBitmark() {
        loadAccount { account ->
            val zeroAddr = Address.fromAccountNumber(BuildConfig.ZERO_ADDRESS)
            val transferParams = TransferParams(zeroAddr, bitmark!!.headId)
            transferParams.sign(account.keyPair)
            viewModel.deleteBitmark(transferParams, bitmark!!.id)
        }
    }

    private fun loadAccount(action: (Account) -> Unit) {
        val accountNumber = bitmark?.accountNumber ?: return
        val spec =
            KeyAuthenticationSpec.Builder(this).setKeyAlias(accountNumber)
                .build()
        Account.loadFromKeyStore(
            this,
            accountNumber,
            spec,
            object : Callback1<Account> {
                override fun onSuccess(account: Account?) {
                    if (account == null) return
                    action.invoke(account)
                }

                override fun onError(throwable: Throwable?) {
                    when (throwable) {

                        // authentication error
                        is AuthenticationException -> {
                            when (throwable.type) {
                                // action cancel authentication
                                AuthenticationException.Type.CANCELLED -> {
                                    dialogController.alert(
                                        R.string.error,
                                        R.string.authentication_required
                                    )
                                }

                                // other cases include error
                                else -> {
                                    dialogController.alert(
                                        getString(R.string.error),
                                        throwable.message!!
                                    )
                                }
                            }
                        }

                        // missing security requirement
                        is AuthenticationRequiredException -> {
                            when (throwable.type) {

                                // did not set up fingerprint/biometric
                                AuthenticationRequiredException.FINGERPRINT, AuthenticationRequiredException.BIOMETRIC -> {
                                    dialogController.alert(
                                        R.string.error,
                                        R.string.fingerprint_required
                                    ) { gotoSecuritySetting() }
                                }

                                // did not set up pass code
                                else -> {
                                    dialogController.alert(
                                        R.string.error,
                                        R.string.passcode_pin_required
                                    ) { gotoSecuritySetting() }
                                }
                            }
                        }
                        else -> {
                            dialogController.alert(
                                R.string.error,
                                R.string.unexpected_error
                            )
                        }
                    }
                }

            })
    }

    private fun gotoSecuritySetting() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        navigator.startActivity(intent)
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }
}