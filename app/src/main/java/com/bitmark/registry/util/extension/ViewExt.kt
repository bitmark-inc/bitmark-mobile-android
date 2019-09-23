package com.bitmark.registry.util.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.*
import android.graphics.Point
import android.graphics.Rect
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.bitmark.apiservice.utils.callback.Callback0
import com.bitmark.apiservice.utils.callback.Callback1
import com.bitmark.registry.R
import com.bitmark.registry.feature.DialogController
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.authentication.Provider
import com.bitmark.sdk.authentication.error.AuthenticationException
import com.bitmark.sdk.authentication.error.AuthenticationRequiredException
import com.bitmark.sdk.features.Account


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

fun View.gone(withAnim: Boolean = false) {
    if (withAnim) {
        animate().alpha(0.0f).setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    visibility = View.GONE
                }
            })
    } else {
        visibility = View.GONE
    }

}

fun View.visible(withAnim: Boolean = false) {
    if (withAnim) {
        animate().alpha(1.0f).setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    visibility = View.VISIBLE
                }
            })
    } else {
        visibility = View.VISIBLE
    }
}

fun View.invisible(withAnim: Boolean = false) {
    if (withAnim) {
        animate().alpha(0.0f).setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    visibility = View.INVISIBLE
                }
            })
    } else {
        visibility = View.INVISIBLE
    }
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
            }, 500)
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

fun TextView.setTextColorRes(@ColorRes id: Int) {
    this.setTextColor(ContextCompat.getColor(context, id))
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

fun Activity.showKeyBoard() {
    val view = this.currentFocus
    if (null != view) {
        val inputManager =
            getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        inputManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

}

fun Activity.loadAccount(
    accountNumber: String,
    spec: KeyAuthenticationSpec,
    dialogController: DialogController,
    successAction: (Account) -> Unit,
    canceledAction: () -> Unit = {},
    setupRequiredAction: () -> Unit = {},
    invalidErrorAction: (Throwable?) -> Unit = {}
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
                        when (throwable.provider) {

                            // did not set up fingerprint/biometric
                            Provider.FINGERPRINT, Provider.BIOMETRIC -> {
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
                        invalidErrorAction.invoke(throwable)
                    }
                }
            }

        })
}

fun Activity.removeAccount(
    accountNumber: String,
    spec: KeyAuthenticationSpec,
    dialogController: DialogController,
    successAction: () -> Unit,
    canceledAction: () -> Unit = {},
    setupRequiredAction: () -> Unit = {},
    invalidErrorAction: (Throwable?) -> Unit = {}
) {
    Account.removeFromKeyStore(this, accountNumber, spec, object : Callback0 {
        override fun onSuccess() {
            successAction.invoke()
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
                    when (throwable.provider) {

                        // did not set up fingerprint/biometric
                        Provider.FINGERPRINT, Provider.BIOMETRIC -> {
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
                    invalidErrorAction.invoke(throwable)
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

fun Context.getResIdentifier(resName: String, classifier: String) = try {
    resources.getIdentifier(resName, classifier, packageName)
} catch (e: Throwable) {
    null
}

fun Context.getString(stringResName: String): String {
    val id = getResIdentifier(stringResName, "string") ?: return ""
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

fun Context.isStorageEncryptionInactive(): Boolean {
    return try {
        val devicePolicyManager =
            getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val status = devicePolicyManager.storageEncryptionStatus
        status == DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE
    } catch (e: Throwable) {
        false
    }
}

fun Activity.getDisplayWidth(): Int {
    val display = windowManager.defaultDisplay
    val size = Point()
    display.getSize(size)
    return size.x
}

fun View.setBackgroundDrawable(@DrawableRes id: Int) {
    background = ContextCompat.getDrawable(context, id)
}

fun TextView.setTextUnderline(text: String) {
    val span = SpannableString(text)
    span.setSpan(
        UnderlineSpan(),
        0,
        text.length,
        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
    )
    setText(span)
}

fun View.getLocationOnScreen(): IntArray {
    val coordinate = IntArray(2)
    getLocationOnScreen(coordinate)
    return coordinate
}
