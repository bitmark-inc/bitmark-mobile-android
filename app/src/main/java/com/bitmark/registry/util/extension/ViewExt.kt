package com.bitmark.registry.util.extension

import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Rect
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.StringRes


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