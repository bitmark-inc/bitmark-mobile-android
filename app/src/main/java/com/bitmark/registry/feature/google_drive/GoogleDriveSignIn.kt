package com.bitmark.registry.feature.google_drive

import android.app.Activity
import android.content.Intent
import com.bitmark.registry.feature.ComponentLifecycleObserver
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes


/**
 * @author Hieu Pham
 * @since 2019-08-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class GoogleDriveSignIn(private val activity: Activity) :
    ComponentLifecycleObserver {

    companion object {
        private const val AUTHORIZE_CODE = 0xAF
    }

    private var signInCallback: SignInCallback? = null

    fun setSignInCallback(callback: SignInCallback) {
        signInCallback = callback
    }

    fun isSignedIn() = GoogleSignIn.getLastSignedInAccount(activity) != null

    fun signIn() {
        val signedInAccount = GoogleSignIn.getLastSignedInAccount(activity)
        if (signedInAccount == null) {
            val options =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail().requestScopes(
                        Scope(DriveScopes.DRIVE_APPDATA)
                    ).build()
            val client = GoogleSignIn.getClient(activity, options)
            activity.startActivityForResult(
                client.signInIntent,
                AUTHORIZE_CODE
            )
        } else {
            signInCallback?.onSignedIn()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        when (requestCode) {
            AUTHORIZE_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleSignInResult(data)
                } else {
                    signInCallback?.onCanceled()
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    private fun handleSignInResult(data: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener {
                signInCallback?.onSignedIn()
            }
            .addOnFailureListener { e -> signInCallback?.onError(e) }
    }

    interface SignInCallback {

        fun onSignedIn()

        fun onCanceled()

        fun onError(e: Throwable)
    }
}