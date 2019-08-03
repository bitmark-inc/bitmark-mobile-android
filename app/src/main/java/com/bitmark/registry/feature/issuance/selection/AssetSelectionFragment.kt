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
import com.bitmark.registry.feature.issuance.issuance.IssuanceActivity
import com.bitmark.registry.util.MediaUtil
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.visible
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
        compositeDisposable.add(rxPermission.request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
            if (granted) action.invoke()
        })
    }

    override fun observe() {
        super.observe()

        viewModel.getAssetInfoLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    handler.postDelayed({
                        progressBar.gone()
                        val asset = res.data() ?: return@postDelayed

                        navigator.anim(Navigator.RIGHT_LEFT).startActivity(
                            IssuanceActivity::class.java,
                            IssuanceActivity.getBundle(asset)
                        )
                        navigator.popChildFragment()
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
    }

    override fun onBackPressed(): Boolean {
        navigator.popChildFragment()
        return super.onBackPressed()
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
                            }.doAfterTerminate {
                                dialogController.dismiss(
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
                                    val path = p.path
                                    viewModel.getAssetInfo(File(path))
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