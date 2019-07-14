package com.bitmark.registry.feature

import android.app.Activity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialog
import java.util.*


/**
 * @author Hieu Pham
 * @since 7/3/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class DialogController(private val activity: Activity) {

    private val queue = ArrayDeque<AppCompatDialog>()

    var showingDialog: AppCompatDialog? = null
        private set

    fun isShowing() = showingDialog != null

    fun alert(
        title: String,
        message: String,
        text: String = activity.getString(android.R.string.ok),
        cancelable: Boolean = false,
        clickEvent: () -> Unit = {}
    ) {
        val dialog =
            AlertDialog.Builder(activity).setTitle(title).setMessage(message)
                .setPositiveButton(text) { d, _ ->
                    d.dismiss()
                    clickEvent.invoke()
                    if (isQueueing()) {
                        val dialog = queue.first
                        dialog.show()
                    }
                }
                .setCancelable(cancelable).create()
        if (isShowing())
            queue.add(dialog)
        else dialog.show()
    }

    fun alert(
        @StringRes title: Int, @StringRes message: Int, @StringRes text: Int = android.R.string.ok,
        cancelable: Boolean = false,
        clickEvent: () -> Unit = {}
    ) {
        val dialog =
            AlertDialog.Builder(activity).setTitle(title).setMessage(message)
                .setPositiveButton(text) { d, _ ->
                    d.dismiss()
                    clickEvent.invoke()
                    if (isQueueing()) {
                        val dialog = queue.first
                        dialog.show()
                    }
                }
                .setCancelable(cancelable).create()
        if (isShowing())
            queue.add(dialog)
        else dialog.show()
    }

    fun confirm(
        title: String,
        message: String,
        cancelable: Boolean = false,
        positive: String = activity.getString(android.R.string.ok),
        positiveEvent: () -> Unit = {},
        negative: String = activity.getString(android.R.string.cancel),
        negativeEvent: () -> Unit = {}
    ) {
        val dialog =
            AlertDialog.Builder(activity).setTitle(title).setMessage(message)
                .setPositiveButton(positive) { d, _ ->
                    d.dismiss()
                    positiveEvent.invoke()
                    if (isQueueing()) {
                        val dialog = queue.first
                        dialog.show()
                    }
                }.setNegativeButton(negative) { d, _ ->
                    d.dismiss()
                    negativeEvent.invoke()
                    if (isQueueing()) {
                        val dialog = queue.first
                        dialog.show()
                    }
                }
                .setCancelable(cancelable).create()
        if (isShowing())
            queue.add(dialog)
        else dialog.show()
    }

    fun confirm(
        @StringRes title: Int,
        @StringRes message: Int,
        cancelable: Boolean = false,
        @StringRes positive: Int = android.R.string.ok,
        positiveEvent: () -> Unit = {},
        @StringRes negative: Int = android.R.string.cancel,
        negativeEvent: () -> Unit = {}
    ) {
        val dialog =
            AlertDialog.Builder(activity).setTitle(title).setMessage(message)
                .setPositiveButton(positive) { d, _ ->
                    d.dismiss()
                    positiveEvent.invoke()
                    if (isQueueing()) {
                        val dialog = queue.first
                        dialog.show()
                    }
                }.setNegativeButton(negative) { d, _ ->
                    d.dismiss()
                    negativeEvent.invoke()
                    if (isQueueing()) {
                        val dialog = queue.first
                        dialog.show()
                    }
                }
                .setCancelable(cancelable).create()
        if (isShowing())
            queue.add(dialog)
        else dialog.show()
    }

    fun dismissShowing() {
        if (isShowing()) showingDialog?.dismiss()
    }

    fun dismiss() {
        dismissShowing()
        while (queue.isNotEmpty()) {
            queue.poll().dismiss()
        }
    }

    private fun isQueueing() = !queue.isEmpty()
}