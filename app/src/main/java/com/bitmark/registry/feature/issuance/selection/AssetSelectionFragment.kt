package com.bitmark.registry.feature.issuance.selection

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.issuance.issuance.IssuanceActivity
import com.bitmark.registry.util.MediaUtil
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.openAppSetting
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.visible
import com.bitmark.registry.util.modelview.AssetModelView
import com.bitmark.registry.util.view.ProgressAppCompatDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_asset_selection.*
import java.io.File
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-30
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AssetSelectionFragment : BaseSupportFragment() {

    companion object {

        private const val BROWSE_CODE = 0x98

        fun newInstance() = AssetSelectionFragment()
    }

    @Inject
    lateinit var viewModel: AssetSelectionViewModel

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var dialogController: DialogController

    private val compositeDisposable = CompositeDisposable()

    private var blocked = false

    private val handler = Handler()

    private var asset: AssetModelView? = null

    override fun layoutRes(): Int = R.layout.fragment_asset_selection

    override fun viewModel(): BaseViewModel? = viewModel

    override fun initComponents() {
        super.initComponents()

        layoutPhoto.setSafetyOnclickListener {
            if (blocked) return@setSafetyOnclickListener
            requestPermission { browseMedia("image/*") }
        }

        layoutVideo.setSafetyOnclickListener {
            if (blocked) return@setSafetyOnclickListener
            requestPermission { browseMedia("video/*") }
        }

        layoutFile.setSafetyOnclickListener {
            if (blocked) return@setSafetyOnclickListener
            requestPermission { browseDocument() }
        }

        ivBack.setOnClickListener { navigator.popChildFragment() }
    }

    override fun deinitComponents() {
        handler.removeCallbacksAndMessages(null)
        dialogController.dismiss()
        compositeDisposable.dispose()
        super.deinitComponents()
    }

    private fun requestPermission(action: () -> Unit) {
        val rxPermission = RxPermissions(this)
        compositeDisposable.add(rxPermission.requestEach(Manifest.permission.READ_EXTERNAL_STORAGE).subscribe { permission ->
            when {
                permission.granted -> action.invoke()
                permission.shouldShowRequestPermissionRationale -> {
                    // do nothing
                }
                else -> {
                    dialogController.alert(
                        R.string.enable_photos_access,
                        R.string.to_get_started_allow_access_photos,
                        R.string.enable_access
                    ) {
                        navigator.openAppSetting(context!!)
                    }
                }
            }
        })
    }

    override fun observe() {
        super.observe()

        viewModel.getAssetInfoLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    asset = res.data() ?: return@Observer
                    handler.postDelayed({
                        progressBar.gone()
                        val asset = res.data() ?: return@postDelayed

                        navigator.anim(RIGHT_LEFT).startActivity(
                            IssuanceActivity::class.java,
                            IssuanceActivity.getBundle(asset)
                        )
                    }, 100)
                }

                res.isLoading() -> {
                    progressBar.visible()
                }

                res.isError() -> {
                    progressBar.gone()
                }
            }
        })

        viewModel.progressLiveData.observe(this, Observer { percent ->
            progressBar.progress = percent
        })

        viewModel.bitmarkSavedLiveData.observe(this, Observer { bitmarks ->
            // FIXME It's not the good solution to exit this screen after issuing
            val assetIds = bitmarks.map { b -> b.assetId }.distinct()
            if (asset != null && assetIds.contains(asset!!.id)) {
                handler.postDelayed({ navigator.popChildFragment() }, 100)
            }
        })
    }

    override fun onBackPressed(): Boolean {
        return navigator.popChildFragment() ?: false
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (resultCode != Activity.RESULT_OK) super.onActivityResult(
            requestCode,
            resultCode,
            data
        )
        else {
            if (data == null) return
            val uri = data.data ?: return
            val progressDialog = ProgressAppCompatDialog(
                context!!,
                title = getString(R.string.extracting_file),
                message = "%s \"%s\"".format(
                    getString(R.string.extracting),
                    uri.path
                )
            )

            when (requestCode) {
                BROWSE_CODE -> {
                    compositeDisposable.add(
                        MediaUtil.getAbsolutePath(
                            context!!,
                            uri
                        ).observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe {
                                dialogController.show(
                                    progressDialog
                                )
                            }.doOnDispose {
                                dialogController.dismiss(
                                    progressDialog
                                )
                            }
                            .subscribe({ p ->
                                Log.d("progress:", p.progress.toString())
                                progressDialog.setProgress(p.progress)
                                if (p.progress >= 100) {
                                    compositeDisposable.clear()
                                    dialogController.dismiss(
                                        progressDialog
                                    )
                                    val path = p.path ?: return@subscribe

                                    val file = File(path)
                                    if (file.length() >= 100 * 1024 * 1024) {
                                        // file reach 100 MB
                                        viewModel.deleteUnusableFile(path)
                                        dialogController.alert(
                                            R.string.error,
                                            R.string.this_asset_is_too_large
                                        )
                                    } else {
                                        viewModel.getAssetInfo(file)
                                    }

                                }
                            }, {
                                dialogController.alert(
                                    R.string.error,
                                    R.string.unsupported_file
                                )
                            })
                    )
                }
            }
        }
    }

    private fun browseMedia(mime: String) {
        val intent = Intent(Intent.ACTION_PICK)
        when (mime) {
            "image/*" -> {
                intent.setDataAndType(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    mime
                )
            }

            "video/*" -> {
                intent.setDataAndType(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    mime
                )
            }
        }
        navigator.startActivityForResult(intent, BROWSE_CODE)
    }

    private fun browseDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        navigator.startActivityForResult(intent, BROWSE_CODE)
    }
}