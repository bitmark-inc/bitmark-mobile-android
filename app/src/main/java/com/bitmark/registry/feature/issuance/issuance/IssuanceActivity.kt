package com.bitmark.registry.feature.issuance.issuance

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
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

        adapter.setItemFilledListener {
            if (adapter.isFilled()) {
                setAddMetadataState(true)
            } else {
                setAddMetadataState(false)
            }
            setRegisterState(
                checkValidData()
            )
        }

        etPropName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) return@setOnFocusChangeListener
            if (etPropName.text?.length!! > MAX_ASSET_NAME_LENGTH) {
                etPropName.background =
                    getDrawable(R.drawable.bg_bottom_line_torch_red)
            } else {
                etPropName.background =
                    getDrawable(R.drawable.bg_bottom_line_blue_ribbon)
            }
        }

        etPropName.doOnTextChanged { text, _, _, _ ->
            if (text?.length!! > MAX_ASSET_NAME_LENGTH) {
                etPropName.background =
                    getDrawable(R.drawable.bg_bottom_line_torch_red)
                etPropName.setTextColorRes(R.color.torch_red)
            } else {
                etPropName.background =
                    getDrawable(R.drawable.bg_bottom_line_blue_ribbon)
                etPropName.setTextColorRes(android.R.color.black)
            }
            setRegisterState(
                checkValidData()
            )
        }

        etIssueQuantity.doOnTextChanged { text, _, _, _ ->
            if (text!!.isNotEmpty()) {
                quantity = text.toString().toInt()
            }
            if (quantity !in (MIN_ASSET_QUANTITY..MAX_ASSET_QUANTITY)) {
                etIssueQuantity.setTextColorRes(R.color.torch_red)
            } else {
                etIssueQuantity.setTextColorRes(android.R.color.black)
            }
            setRegisterState(
                checkValidData()
            )
        }

        cbRightsClaim.setOnCheckedChangeListener { _, _ ->
            setRegisterState(
                checkValidData()
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

        tvWhatIsPropDes.setSafetyOnclickListener { }

        btnRegister.setSafetyOnclickListener {
            if (blocked) return@setSafetyOnclickListener

            hideKeyBoard()
            val assetId = asset.id
            val registered = asset.registered
            val file = File(asset.filePath)
            val propName = etPropName.text.toString()
            val metadata = adapter.toMap()
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
            adapter.add()
            setAddMetadataState(false)
            setActionMetadataState(true, getString(R.string.edit))
        }

        ivBack.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).finishActivity()
        }

        tvAssetType.setOnClickListener {
            tvAssetType.isSelected = !tvAType.isSelected
            tvAssetType.background =
                getDrawable(if (tvAssetType.isSelected) R.drawable.bg_border_blue_ribbon else R.drawable.bg_border_torch_red)
            if (tvAssetType.isSelected) showAssetTypePopupMenu()
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

    override fun observe() {
        super.observe()

        viewModel.registerPropertyLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    blocked = false
                    progressBar.gone()
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
                    progressBar.visible()
                }
            }
        })

        viewModel.progressLiveData.observe(this, Observer { percent ->
            progressBar.progress = percent
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
            .setAuthenticationDescription(getString(R.string.please_sign_to_register_property_rights))
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

    private fun showAssetTypePopupMenu() {
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
        val adapter = SimpleRecyclerViewAdapter()

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
            setRegisterState(checkValidData())
        }

    }

    private fun setRegisterState(enable: Boolean) {
        val drawable: Drawable
        if (enable) {
            drawable = ContextCompat.getDrawable(
                this,
                R.drawable.bg_blue_ribbon_stroke
            )!!
            btnRegister.enable()
        } else {
            drawable =
                ContextCompat.getDrawable(this, R.drawable.bg_silver_stroke)!!
            btnRegister.disable()
        }
        btnRegister.setCompoundDrawablesWithIntrinsicBounds(
            null,
            drawable,
            null,
            null
        )
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

    private fun checkValidData(): Boolean {
        val propertyName = etPropName.text.toString()
        val validPropertyName =
            propertyName.isEmpty() || propertyName.length <= MAX_ASSET_NAME_LENGTH
        val validAssetType = !assetType.isNullOrEmpty()
        val validMetadata = adapter.isValid()
        val validQuantity = quantity in (MIN_ASSET_QUANTITY..MAX_ASSET_QUANTITY)
        val validRightClaims = cbRightsClaim.isChecked
        return validPropertyName && validAssetType && validMetadata && validQuantity && validRightClaims
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

}