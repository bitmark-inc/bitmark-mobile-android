package com.bitmark.registry.feature.account.details

import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.util.DateTimeUtil
import kotlinx.android.synthetic.main.activity_whats_new.*
import java.util.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-29
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class WhatsNewActivity : BaseAppCompatActivity() {

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
        tvNotes.text = releaseNoteInfo[0]
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