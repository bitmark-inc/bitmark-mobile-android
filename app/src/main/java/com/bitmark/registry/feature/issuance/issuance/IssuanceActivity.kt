package com.bitmark.registry.feature.issuance.issuance

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.AssetModelView
import com.bitmark.registry.util.view.InfoAppCompatDialog
import com.bitmark.registry.util.view.SimpleRecyclerViewAdapter
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_issuance.*
import java.io.File
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-31
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class IssuanceActivity : BaseAppCompatActivity() {

    companion object {
        private const val ASSET = "asset"
        private const val MIN_ASSET_NAME_LENGTH = 1
        private const val MAX_ASSET_NAME_LENGTH = 64
        private const val MIN_ASSET_QUANTITY = 1
        private const val MAX_ASSET_QUANTITY = 100

        fun getBundle(asset: AssetModelView): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(ASSET, asset)
            return bundle
        }
    }

    @Inject
    lateinit var viewModel: IssuanceViewModel

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var dialogController: DialogController

    private val adapter = MetadataRecyclerViewAdapter()

    private var assetType: String? = null

    private var quantity = MIN_ASSET_QUANTITY

    private var propName: String? = null

    private lateinit var accountInfo: Pair<String, String>

    private val handler = Handler()

    private var blocked = false

    override fun layoutRes(): Int = R.layout.activity_issuance

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        viewModel.getAccountInfo()
    }

    override fun initComponents() {
        super.initComponents()

        val bundle = intent?.extras!!

        val asset = bundle.getParcelable<AssetModelView>(ASSET)!!

        tvWhatIsPropDes.setTextUnderline(getString(R.string.what_is_property_description_arrow))
        setRegisterState(false)
        setAddMetadataState(false)
        setActionMetadataState(false)
        setAddMetadataVisibility(false)
        setActionMetadataVisibility(false)

        tvFingerprint.text = asset.fingerprint
        tvFileName.text = asset.fileName
        etPropName.setText(asset.name ?: "")
        adapter.set(asset.metadata ?: mapOf())
        etIssueQuantity.setText(quantity.toString())

        val layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rvMetadata.isNestedScrollingEnabled = false
        rvMetadata.layoutManager = layoutManager
        rvMetadata.adapter = adapter

        if (asset.registered) {
            adapter.disable()
            etPropName.isFocusable = false
            setAddMetadataState(false)
            setActionMetadataState(false)
            tvAssetType.disable()
            assetType =
                asset.metadata?.get("source") ?: asset.metadata?.get("Source")
                        ?: getString(R.string.other)
            tvAssetType.text = assetType
            tvAssetType.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_arrow_down_inactive,
                0
            )
        }

        adapter.setItemFilledListener { filled ->
            if (adapter.hasSingleRow()) {
                setActionMetadataVisibility(filled)
                setAddMetadataVisibility(filled)
            }
            if (!adapter.isRemoving() && adapter.hasValidRows() && !asset.registered) {
                setAddMetadataState(true)
            } else {
                setAddMetadataState(false)
            }
            setRegisterState(
                checkValidData(asset.registered)
            )
        }

        adapter.setItemRemovedListener {
            val removable = adapter.isRemovable()
            if (!removable) {
                tvActionMetadata.setText(R.string.edit)
                setActionMetadataState(false)
                setAddMetadataVisibility(false)
                setActionMetadataVisibility(false)
            }
            adapter.changeRemovableState(removable)
        }

        etPropName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && propName == null) {
                etPropName.background =
                    getDrawable(R.drawable.bg_bottom_line_blue_ribbon)
            } else if (!hasFocus && !isPropNameValid(etPropName.text.toString())) {
                etPropName.background =
                    getDrawable(R.drawable.bg_bottom_line_torch_red)
            }
        }

        etPropName.doOnTextChanged { text, _, _, _ ->
            val isClear = text.isNullOrEmpty() && !propName.isNullOrEmpty()
            propName = text.toString()
            if (isClear || text!!.length > MAX_ASSET_NAME_LENGTH) {
                etPropName.background =
                    getDrawable(R.drawable.bg_bottom_line_torch_red)
                etPropName.setTextColorRes(R.color.torch_red)
            } else {
                etPropName.background =
                    getDrawable(R.drawable.bg_bottom_line_blue_ribbon)
                etPropName.setTextColorRes(android.R.color.black)
            }
            setRegisterState(
                checkValidData(asset.registered)
            )
        }

        etIssueQuantity.doOnTextChanged { text, _, _, _ ->
            quantity = if (text!!.isNotEmpty()) {
                text.toString().toInt()
            } else {
                MIN_ASSET_QUANTITY
            }

            if (quantity !in (MIN_ASSET_QUANTITY..MAX_ASSET_QUANTITY)) {
                etIssueQuantity.setTextColorRes(R.color.torch_red)
            } else {
                etIssueQuantity.setTextColorRes(android.R.color.black)
            }
            setRegisterState(
                checkValidData(asset.registered)
            )
        }

        etIssueQuantity.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                etIssueQuantity.setText(quantity.toString())
            }
        }

        cbRightsClaim.setOnCheckedChangeListener { _, _ ->
            setRegisterState(
                checkValidData(asset.registered)
            )
        }

        ivPlus.setOnClickListener {
            if (quantity == MAX_ASSET_QUANTITY) return@setOnClickListener
            etIssueQuantity.setText((++quantity).toString())
        }

        ivMinus.setOnClickListener {
            if (quantity == MIN_ASSET_QUANTITY) return@setOnClickListener
            etIssueQuantity.setText((--quantity).toString())
        }

        tvWhatIsPropDes.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT)
                .startActivity(PropertyDescriptionActivity::class.java)
        }

        btnRegister.setSafetyOnclickListener {
            if (blocked) return@setSafetyOnclickListener

            hideKeyBoard()
            val assetId = asset.id
            val registered = asset.registered
            val file = File(asset.filePath)
            val propName = etPropName.text.toString().trim()
            val metadata = adapter.toMap().toMutableMap()
            metadata["source"] = assetType!!
            val quantity = etIssueQuantity.text.toString().toInt()

            loadKey(accountInfo.first, accountInfo.second) { keyPair ->
                viewModel.registerProperty(
                    assetId,
                    propName,
                    metadata,
                    file,
                    quantity,
                    registered,
                    keyPair
                )
            }
        }

        tvActionMetadata.setOnClickListener {
            if (tvActionMetadata.text.toString() == getString(R.string.edit)) {
                // edit
                adapter.changeRemovableState(adapter.isRemovable())
                setAddMetadataState(false)
                setActionMetadataState(true, getString(R.string.done))
            } else {
                // done
                adapter.changeRemovableState(false)
                setAddMetadataState(true)
                setActionMetadataState(
                    adapter.isRemovable(),
                    getString(R.string.edit)
                )
            }
        }

        tvAddMetadata.setOnClickListener {
            adapter.add(true)
            setAddMetadataState(false)
            setActionMetadataState(true, getString(R.string.edit))
        }

        ivBack.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).finishActivity()
        }

        tvAssetType.setOnClickListener {
            tvAssetType.background =
                getDrawable(R.drawable.bg_border_blue_ribbon)
            tvAssetType.setTextColorRes(R.color.blue_ribbon)
            tvAssetType.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_arrow_down_2,
                0
            )
            showAssetTypePopupMenu(asset.registered)
        }

        layoutContent.setOnTouchListener(object : View.OnTouchListener {

            var startX = 0f
            var startY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                    }

                    MotionEvent.ACTION_UP -> {
                        val endX = event.x
                        val endY = event.y

                        if (Math.abs(startX - endX) < 100 && Math.abs(startY - endY) < 100) {
                            if (currentFocus is EditText) {
                                hideKeyBoard()
                                currentFocus?.clearFocus()
                            }
                        }
                    }
                }
                return true
            }

        })
    }

    override fun deinitComponents() {
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()

        viewModel.registerPropertyLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    blocked = false
                    val dialog = InfoAppCompatDialog(
                        this,
                        getString(R.string.your_rights_to_this_property)
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
                    blocked = false
                    progressBar.gone()
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_register_property
                    )
                }

                res.isLoading() -> {
                    blocked = true
                    val snackbar = Snackbar.make(
                        btnRegister,
                        R.string.registering_your_rights_three_dot,
                        Snackbar.LENGTH_SHORT
                    )
                    val view = snackbar.view
                    view.background =
                        getDrawable(R.drawable.bg_wild_sand_shadow)
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        ?.setTextColorRes(android.R.color.black)
                    snackbar.show()
                    progressBar.visible()
                }
            }
        })

        viewModel.progressLiveData.observe(this, Observer { percent ->
            progressBar.progress = percent
            if (percent >= 100) {
                progressBar.gone()
            }
        })

        viewModel.getAccountNumberLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    accountInfo = res.data()!!
                }

                res.isError() -> {
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error
                    ) { navigator.anim(RIGHT_LEFT).finishActivity() }
                }
            }
        })
    }

    private fun loadKey(
        accountNumber: String,
        keyAlias: String,
        action: (KeyPair) -> Unit
    ) {
        val spec = KeyAuthenticationSpec.Builder(this)
            .setKeyAlias(keyAlias)
            .setUsePossibleAlternativeAuthentication(true)
            .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
            .build()
        loadAccount(
            accountNumber,
            spec,
            dialogController,
            successAction = { account -> action.invoke(account.keyPair) },
            setupRequiredAction = { navigator.gotoSecuritySetting() },
            unknownErrorAction = {
                dialogController.alert(
                    R.string.error,
                    R.string.unexpected_error
                )
            })
    }

    private fun showAssetTypePopupMenu(registered: Boolean) {
        val recyclerView = RecyclerView(this)
        val rvLayoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.MATCH_PARENT
        )
        recyclerView.layoutParams = rvLayoutParams

        val layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val itemDecoration =
            DividerItemDecoration(this, layoutManager.orientation)
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(itemDecoration)
        val adapter =
            SimpleRecyclerViewAdapter(
                layoutItemRes = R.layout.item_simple_recycler_view_2,
                itemBackground = R.drawable.bg_border_bottom_top_less_white_stateful
            )

        val assetTypes =
            resources?.getStringArray(R.array.asset_type)?.toList()!!
        adapter.add(assetTypes)
        recyclerView.adapter = adapter

        val rootLayout = LinearLayout(this)
        rootLayout.background = getDrawable(android.R.color.white)
        rootLayout.addView(recyclerView)

        val width = getDisplayWidth() - 2 * getDimensionPixelSize(R.dimen.dp_20)
        val popupWindow = PopupWindow(
            rootLayout,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        popupWindow.elevation = 25f
        popupWindow.isFocusable = true
        popupWindow.setOnDismissListener {
            tvAssetType.isSelected = false
            tvAssetType.background =
                getDrawable(if (tvAssetType.text.isEmpty()) R.drawable.bg_border_torch_red else R.drawable.bg_border_blue_ribbon)
        }

        popupWindow.showAsDropDown(tvAssetType)

        adapter.setItemClickListener { item ->
            assetType = item
            tvAssetType.text = assetType
            tvAssetType.setTextColorRes(R.color.blue_ribbon)
            tvAssetType.background =
                getDrawable(R.drawable.bg_border_blue_ribbon)
            popupWindow.dismiss()
            setRegisterState(checkValidData(registered))
        }

    }

    private fun setRegisterState(enable: Boolean) {
        if (enable) {
            btnRegister.enable()
        } else {
            btnRegister.disable()
        }
    }

    private fun setAddMetadataState(enable: Boolean) {
        if (enable) {
            tvAddMetadata.setTextColorRes(R.color.blue_ribbon)
            ivAdd.setImageResource(R.drawable.ic_add)
            tvAddMetadata.enable()
        } else {
            tvAddMetadata.setTextColorRes(R.color.silver)
            ivAdd.setImageResource(R.drawable.ic_add_inactive)
            tvAddMetadata.disable()
        }
    }

    private fun setAddMetadataVisibility(visible: Boolean) {
        if (visible) {
            tvAddMetadata.visible()
            ivAdd.visible()
        } else {
            tvAddMetadata.gone()
            ivAdd.gone()
        }
    }

    private fun setActionMetadataState(enable: Boolean, text: String? = null) {
        if (text != null) {
            tvActionMetadata.text = text
        }
        if (enable) {
            tvActionMetadata.setTextColorRes(R.color.blue_ribbon)
            tvActionMetadata.enable()
        } else {
            tvActionMetadata.setTextColorRes(R.color.silver)
            tvActionMetadata.disable()
        }
    }

    private fun setActionMetadataVisibility(visible: Boolean) {
        if (visible) {
            tvActionMetadata.visible()
        } else {
            tvActionMetadata.gone()
        }
    }

    private fun checkValidData(registered: Boolean): Boolean {
        val propertyName = etPropName.text.toString()
        val validPropertyName = isPropNameValid(propertyName)
        val validAssetType =
            if (registered) true else !assetType.isNullOrEmpty()
        val validMetadata = adapter.isValid()
        val validQuantity = quantity in (MIN_ASSET_QUANTITY..MAX_ASSET_QUANTITY)
        val validRightClaims = cbRightsClaim.isChecked
        return validPropertyName && validAssetType && validMetadata && validQuantity && validRightClaims
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }

    private fun isPropNameValid(name: String) =
        !name.isBlank() && name.length in MIN_ASSET_NAME_LENGTH..MAX_ASSET_NAME_LENGTH

}