package com.bitmark.registry.feature.properties

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.View
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.issuance.selection.AssetSelectionFragment
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_properties.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/8/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PropertiesFragment : BaseSupportFragment() {

    @Inject
    internal lateinit var viewModel: PropertiesViewModel

    @Inject
    internal lateinit var navigator: Navigator

    private lateinit var adapter: PropertiesViewPagerAdapter

    private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {

        override fun onTabReselected(p0: TabLayout.Tab?) {
            (adapter.currentFragment as? BaseSupportFragment)?.refresh()
        }

        override fun onTabUnselected(p0: TabLayout.Tab?) {
        }

        override fun onTabSelected(p0: TabLayout.Tab?) {
        }

    }

    companion object {
        fun newInstance(): PropertiesFragment = PropertiesFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getBitmarkCount()
    }

    override fun viewModel(): BaseViewModel? = viewModel

    override fun layoutRes(): Int = R.layout.fragment_properties

    override fun initComponents() {
        super.initComponents()

        adapter = PropertiesViewPagerAdapter(context, childFragmentManager)
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        ivAdd.setOnClickListener {
            navigator.anim(RIGHT_LEFT).replaceChildFragment(
                R.id.layoutContainer,
                AssetSelectionFragment.newInstance()
            )
        }

        tabLayout.addOnTabSelectedListener(tabSelectedListener)
    }

    override fun deinitComponents() {
        tabLayout.removeOnTabSelectedListener(tabSelectedListener)
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()
        viewModel.getBitmarkCountLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val count = res.data()
                    val displayCount = "%s(%s)".format(
                        getString(R.string.yours).toUpperCase(),
                        if (count!! <= 99) count.toString() else "99+"
                    )
                    val span = SpannableString(displayCount)
                    span.setSpan(
                        RelativeSizeSpan(0.7f),
                        displayCount.indexOfFirst { c -> c == '(' },
                        displayCount.length,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    tabLayout.getTabAt(0)?.text = span
                }

                else -> {
                    // do nothing
                }
            }
        })
    }

    override fun refresh() {
        super.refresh()
        viewPager.currentItem = 0
    }
}