package com.bitmark.registry.feature.cloud_service_sign_in

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.feature.sync.GoogleDriveService
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import kotlinx.android.synthetic.main.activity_cloud_service_sign_in.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-08-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class CloudServiceSignInActivity : BaseAppCompatActivity() {

    companion object {
        private const val FIRST_LAUNCH = "first_launch"

        fun getBundle(isFirstLaunch: Boolean = false): Bundle {
            val bundle = Bundle()
            bundle.putBoolean(FIRST_LAUNCH, isFirstLaunch)
            return bundle
        }
    }

    @Inject
    internal lateinit var viewModel: CloudServiceSignInViewModel

    @Inject
    internal lateinit var navigator: Navigator

    @Inject
    internal lateinit var dialogController: DialogController

    @Inject
    internal lateinit var googleDriveService: GoogleDriveService

    private var isFirstLaunch: Boolean = false

    override fun layoutRes(): Int =
        R.layout.activity_cloud_service_sign_in

    override fun viewModel(): BaseViewModel? = viewModel

    override fun initComponents() {
        super.initComponents()

        isFirstLaunch = intent?.extras?.getBoolean(FIRST_LAUNCH) ?: false

        addLifecycleObserver(googleDriveService)

        googleDriveService.setSignInCallback(object :
            GoogleDriveService.SignInCallback {
            override fun onSignedIn() {
                viewModel.setCloudServiceRequired(false)
            }

            override fun onCanceled() {
                // do nothing
            }

            override fun onError(e: Throwable) {
                dialogController.alert(
                    getString(R.string.error),
                    e.message ?: getString(R.string.unexpected_error)
                )
            }

        })

        btnAuthorize.setSafetyOnclickListener { googleDriveService.signIn() }

        btnSkip.setSafetyOnclickListener {
            viewModel.setCloudServiceRequired(true)
        }
    }

    override fun observe() {
        super.observe()

        viewModel.setCloudServiceRequiredLiveData()
            .observe(this, Observer { res ->
                when {
                    res.isSuccess() -> {
                        transit(isFirstLaunch)
                    }

                    res.isError() -> {
                        dialogController.alert(
                            R.string.error,
                            R.string.unexpected_error
                        )
                    }
                }
            })
    }

    private fun transit(firstLaunch: Boolean) {
        if (firstLaunch) {
            val intent = Intent(this, MainActivity::class.java)
            val uri = this.intent?.data
            if (uri != null) intent.data = uri
            navigator.anim(RIGHT_LEFT).startActivityAsRoot(intent)
        } else {
            navigator.anim(BOTTOM_UP).finishActivity()
        }
    }

    override fun onBackPressed() {
        navigator.anim(if (isFirstLaunch) RIGHT_LEFT else BOTTOM_UP)
            .finishActivity()
        super.onBackPressed()
    }
}