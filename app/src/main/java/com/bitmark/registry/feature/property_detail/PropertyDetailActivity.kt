package com.bitmark.registry.feature.property_detail

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.BitmarkModelView
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
                popupWindow.dismiss()
            }
            item2.setOnClickListener {
                tvSubItem2.visible()
                Handler().postDelayed({
                    tvSubItem2.invisible()
                    popupWindow.dismiss()
                }, 1000)
            }
            item3.setOnClickListener { popupWindow.dismiss() }
            item4.setOnClickListener { popupWindow.dismiss() }
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

                res.isError() -> {
                    // TODO remove
                    if (BuildConfig.DEBUG) throw res.throwable()!!
                }
            }
        })

        viewModel.syncProvenanceLiveData().observe(this, Observer { res ->
            when {
                res.isLoading() -> {
                    progressBar.visible()
                }

                res.isError() -> {
                    progressBar.gone()
                    // TODO remove
                    if (BuildConfig.DEBUG) throw res.throwable()!!
                }

                res.isSuccess() -> {
                    progressBar.gone()
                    val p = res.data() ?: return@Observer
                    provenanceAdapter.set(p.first, p.second)
                }
            }
        })
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }
}