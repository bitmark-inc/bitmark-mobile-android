package com.bitmark.registry.util.extension

import android.os.Handler
import android.view.View


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