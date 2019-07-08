package com.bitmark.registry.feature.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.main.account.AccountFragment
import com.bitmark.registry.feature.main.properties.PropertiesFragment
import com.bitmark.registry.feature.main.transactions.TransactionsFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initComponents()
    }

    private fun initComponents() {
        val navAdapter = AHBottomNavigationAdapter(this, R.menu.navigation)
        navAdapter.setupWithBottomNavigation(bottomNav)
        bottomNav.accentColor =
            ContextCompat.getColor(this, R.color.blue_ribbon)
        bottomNav.inactiveColor =
            ContextCompat.getColor(this, R.color.dusty_gray)
        bottomNav.setTitleTypeface(
            ResourcesCompat.getFont(this, R.font.avenir_next_w1g_bold)
        )
        bottomNav.setTitleTextSizeInSp(10f, 10f)

        val adapter = MainViewPagerAdapter(supportFragmentManager)
        adapter.add(
            PropertiesFragment.newInstance(),
            TransactionsFragment.newInstance(),
            AccountFragment.newInstance()
        )
        viewPager.adapter = adapter
        viewPager.setCurrentItem(0, false)


        bottomNav.setOnTabSelectedListener { position, wasSelected ->
            viewPager.setCurrentItem(position, false)

            if (wasSelected) {
                (adapter.currentFragment as? BaseSupportFragment)?.refresh()
            }

            true
        }

    }
}
