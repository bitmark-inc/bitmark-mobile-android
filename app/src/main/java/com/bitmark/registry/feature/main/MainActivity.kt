package com.bitmark.registry.feature.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.BehaviorComponent
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.account.AccountContainerFragment
import com.bitmark.registry.feature.properties.PropertiesContainerFragment
import com.bitmark.registry.feature.property_detail.PropertyDetailContainerActivity
import com.bitmark.registry.feature.splash.SplashActivity
import com.bitmark.registry.feature.transactions.TransactionsFragment
import com.bitmark.registry.util.modelview.BitmarkModelView
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : BaseAppCompatActivity() {

    @Inject
    lateinit var viewModel: MainViewModel

    @Inject
    lateinit var navigator: Navigator

    private lateinit var adapter: MainViewPagerAdapter

    private val handler = Handler()

    override fun layoutRes(): Int = R.layout.activity_main

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.checkUnseenBitmark()
        viewModel.checkActionRequired()
        val notificationBundle = intent?.getBundleExtra("notification")
        val isDirectFromNotification =
            intent?.getBooleanExtra("direct_from_notification", false) ?: false
        if (notificationBundle != null) {
            if (isDirectFromNotification) {
                val bundle = Bundle()
                bundle.putBundle("notification", notificationBundle)
                navigator.startActivityAsRoot(
                    SplashActivity::class.java,
                    bundle
                )
            } else {
                handleNotification(notificationBundle)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val notificationBundle = intent?.getBundleExtra("notification")
        if (notificationBundle != null) {
            handleNotification(notificationBundle)
        }
    }

    private fun handleNotification(bundle: Bundle) {
        val event = bundle.getString("event")
        if (event != null && event == "intercom_reply") {
            // a bit delay for waiting view inflating
            handler.postDelayed({ openIntercom() }, 100)
        } else {
            when (bundle.getString("name")) {
                "transfer_confirmed_receiver", "transfer_failed" -> {
                    val bitmarkId = bundle.getString("bitmark_id")
                    if (bitmarkId.isNullOrEmpty()) return
                    viewModel.getBitmark(bitmarkId)
                }

                "claim_request_rejected" -> {
                    openTxHistory()
                }

                "claim_request" -> {
                    // TODO
                }
            }
        }
    }

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

    override fun deinitComponents() {
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()
        viewModel.checkBitmarkSeenLiveData.observe(this, Observer { has ->
            if (has) {
                bottomNav.setNotification(" ", 0)
            } else {
                bottomNav.setNotification("", 0)
            }
        })

        viewModel.checkActionRequiredLiveData.observe(this, Observer { count ->
            if (count > 0) {
                bottomNav.setNotification(count.toString(), 1)
            } else {
                bottomNav.setNotification("", 1)
            }
        })

        viewModel.getBitmarkLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val bitmark = res.data() ?: return@Observer
                    openPropertyDetail(bitmark)
                }
            }
        })
    }

    override fun onBackPressed() {
        val currentFragment = adapter.currentFragment as? BehaviorComponent
        if (currentFragment is PropertiesContainerFragment)
            super.onBackPressed()
        else if (currentFragment?.onBackPressed() == false) {
            bottomNav.currentItem = 0
            viewPager.setCurrentItem(0, false)
        }
    }

    private fun switchTab(pos: Int) {
        bottomNav.currentItem = pos
        viewPager.setCurrentItem(pos, false)
    }

    fun openRecoveryPhraseWarning() {
        switchTab(MainViewPagerAdapter.TAB_ACCOUNT)
        val accountFragment =
            adapter.currentFragment as? AccountContainerFragment
        accountFragment?.gotoRecoveryPhraseWarning()
    }

    private fun openIntercom() {
        switchTab(MainViewPagerAdapter.TAB_ACCOUNT)
        val accountContainerFragment =
            adapter.currentFragment as? AccountContainerFragment
        accountContainerFragment?.openIntercom()
    }

    private fun openPropertyDetail(bitmark: BitmarkModelView) {
        switchTab(MainViewPagerAdapter.TAB_PROPERTIES)
        navigator.startActivity(
            PropertyDetailContainerActivity::class.java,
            PropertyDetailContainerActivity.getBundle(bitmark)
        )
    }

    private fun openTxHistory() {
        switchTab(MainViewPagerAdapter.TAB_TXS)
        (adapter.currentFragment as? TransactionsFragment)?.openTxHistory()
    }
}
