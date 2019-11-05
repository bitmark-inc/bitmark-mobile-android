/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.account.details

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.util.DateTimeUtil
import com.bitmark.registry.util.extension.openMail
import kotlinx.android.synthetic.main.activity_whats_new.*
import java.util.*
import javax.inject.Inject

class WhatsNewActivity : BaseAppCompatActivity() {

    companion object {
        private const val SUPPORT_EMAIL = "support@bitmark.com"
    }

    @Inject
    lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.activity_whats_new

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

        tvVersion.text = "%s %s".format(
            getString(R.string.version),
            BuildConfig.VERSION_NAME
        )
        val releaseNoteInfo =
            resources?.getStringArray(R.array.release_note_info) ?: arrayOf(
                "",
                ""
            )

        val releaseNote = releaseNoteInfo[0].format(SUPPORT_EMAIL)
        val spannable = SpannableString(releaseNote)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigator.openMail(this@WhatsNewActivity, SUPPORT_EMAIL)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }

        }

        val startPos = releaseNote.indexOf(SUPPORT_EMAIL)
        spannable.setSpan(
            clickableSpan,
            startPos,
            startPos + SUPPORT_EMAIL.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        tvNotes.text = spannable
        tvNotes.movementMethod = LinkMovementMethod.getInstance()
        tvNotes.highlightColor = Color.TRANSPARENT

        tvDate.text = getString(R.string.day_ago_format).format(
            DateTimeUtil.dayCountFrom(
                DateTimeUtil.stringToDate(releaseNoteInfo[1]) ?: Date()
            )
        )

        tvClose.setOnClickListener {
            navigator.anim(BOTTOM_UP).finishActivity()
        }
    }

    override fun onBackPressed() {
        navigator.anim(BOTTOM_UP).finishActivity()
        super.onBackPressed()
    }
}