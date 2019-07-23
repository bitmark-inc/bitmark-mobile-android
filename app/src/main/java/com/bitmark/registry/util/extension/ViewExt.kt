package com.bitmark.registry.util.extension

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Rect
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import com.bitmark.apiservice.utils.callback.Callback1
import com.bitmark.registry.R
import com.bitmark.registry.feature.DialogController
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.authentication.error.AuthenticationException
import com.bitmark.sdk.authentication.error.AuthenticationRequiredException
import com.bitmark.sdk.features.Account


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.setSafetyOnclickListener(action: (View?) -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {

        var blocked = false

        val handler = Handler()

        override fun onClick(v: View?) {
            if (blocked) return

            blocked = true
            handler.postDelayed({
                blocked = false
            }, 1000)
            action.invoke(v)
        }

    })
}

fun View.enable() {
    this.isEnabled = true
}

fun View.disable() {
    this.isEnabled = false
}

fun TextView.setText(@StringRes id: Int) {
    this.text = context.getString(id)
}

fun Activity.detectKeyBoardState(action: (Boolean) -> Unit) {
    val contentView = findViewById<View>(android.R.id.content)
    var isShowing = false
    contentView.viewTreeObserver.addOnGlobalLayoutListener {
        val rect = Rect()
        contentView.getWindowVisibleDisplayFrame(rect)
        val screenHeight = contentView.rootView.height
        val keyboardHeight = screenHeight - rect.bottom
        if (keyboardHeight > screenHeight * 0.15) {
            if (!isShowing) {
                isShowing = true
                action.invoke(true)
            }
        } else {
            if (isShowing) {
                isShowing = false
                action.invoke(false)
            }
        }
    }
}

fun Activity.hideKeyBoard() {
    val view = this.currentFocus
    if (null != view) {
        val inputManager =
            getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        inputManager?.hideSoftInputFromWindow(view.windowToken, 0)
    }

}

fun Activity.loadAccount(
    accountNumber: String,
    spec: KeyAuthenticationSpec,
    dialogController: DialogController,
    successAction: (Account) -> Unit,
    canceledAction: () -> Unit = {},
    setupRequiredAction: () -> Unit = {},
    unknownErrorAction: (Throwable?) -> Unit = {}
) {
    Account.loadFromKeyStore(
        this,
        accountNumber,
        spec,
        object : Callback1<Account> {
            override fun onSuccess(acc: Account?) {
                successAction.invoke(acc!!)
            }

            override fun onError(throwable: Throwable?) {
                when (throwable) {

                    // authentication error
                    is AuthenticationException -> {
                        when (throwable.type) {
                            // action cancel authentication
                            AuthenticationException.Type.CANCELLED -> {
                                canceledAction.invoke()
                            }

                            else -> {
                                // do nothing
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
                                ) { setupRequiredAction.invoke() }
                            }

                            // did not set up pass code
                            else -> {
                                dialogController.alert(
                                    R.string.error,
                                    R.string.passcode_pin_required
                                ) { setupRequiredAction.invoke() }
                            }
                        }
                    }
                    else -> {
                        unknownErrorAction.invoke(throwable)
                    }
                }
            }

        })
}

fun Context.copyToClipboard(text: String) {
    val clipboardManager =
        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("", text)
    clipboardManager.primaryClip = clip
}

fun Context.getResIdentifier(resName: String, classifier: String): Int {
    return resources.getIdentifier(resName, classifier, packageName)
}

fun Context.getString(stringResName: String): String {
    val id = getResIdentifier(stringResName, "string")
    return try {
        getString(id)
    } catch (e: Throwable) {
        ""
    }
}

fun Context.getDimensionPixelSize(@DimenRes dimenRes: Int): Int {
    return try {
        resources.getDimensionPixelSize(dimenRes)
    } catch (e: Throwable) {
        0
    }
}

fun Context.getDimension(@DimenRes dimenRes: Int, default: Float = 0f): Float {
    return try {
        resources.getDimension(dimenRes)
    } catch (e: Throwable) {
        default
    }
}