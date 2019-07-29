package com.bitmark.registry.feature.main

import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.BehaviorComponent
import com.bitmark.registry.feature.account.AccountContainerFragment
import com.bitmark.registry.feature.properties.PropertiesFragment
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : BaseAppCompatActivity() {

    @Inject
    lateinit var viewModel: MainViewModel

    private lateinit var adapter: MainViewPagerAdapter

    override fun layoutRes(): Int = R.layout.activity_main

    override fun viewModel(): BaseViewModel? = viewModel

    override fun initComponents() {
        super.initComponents()
        val navAdapter = AHBottomNavigationAdapter(this, R.menu.navigation)
        navAdapter.setupWithBottomNavigation(bottomNav)
        bottomNav.defaultBackgroundColor = ContextCompat.getColor(
            this,
            R.color.wild_sand
        )
        bottomNav.accentColor =
            ContextCompat.getColor(this, R.color.blue_ribbon)
        bottomNav.inactiveColor =
            ContextCompat.getColor(this, R.color.dusty_gray)
        bottomNav.setTitleTypeface(
            ResourcesCompat.getFont(this, R.font.avenir_next_w1g_bold)
        )


        adapter = MainViewPagerAdapter(supportFragmentManager)
        viewPager.offscreenPageLimit = adapter.count
        viewPager.adapter = adapter
        viewPager.setCurrentItem(0, false)


        bottomNav.setOnTabSelectedListener { position, wasSelected ->
            viewPager.setCurrentItem(position, false)

            if (wasSelected) {
                (adapter.currentFragment as? BehaviorComponent)?.refresh()
            }

            true
        }

    }

    override fun onBackPressed() {
        val currentFragment = adapter.currentFragment as? BehaviorComponent
        if (currentFragment is PropertiesFragment)
            super.onBackPressed()
        else if (currentFragment?.onBackPressed() == false) {
            bottomNav.currentItem = 0
            viewPager.setCurrentItem(0, false)
        }
    }

    fun switchTab(pos: Int) {
        bottomNav.currentItem = pos
        viewPager.setCurrentItem(pos, false)
    }

    fun gotoRecoveryPhraseWarning() {
        switchTab(MainViewPagerAdapter.TAB_ACCOUNT)
        val accountFragment =
            adapter.currentFragment as? AccountContainerFragment
        accountFragment?.gotoRecoveryPhraseWarning()
    }
}
