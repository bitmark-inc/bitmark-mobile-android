package com.bitmark.registry.feature.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter
import com.bitmark.registry.AppLifecycleHandler
import com.bitmark.registry.R
import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.feature.*
import com.bitmark.registry.feature.account.AccountContainerFragment
import com.bitmark.registry.feature.cloud_service_sign_in.CloudServiceSignInActivity
import com.bitmark.registry.feature.connectivity.ConnectivityHandler
import com.bitmark.registry.feature.google_drive.GoogleDriveSignIn
import com.bitmark.registry.feature.properties.PropertiesContainerFragment
import com.bitmark.registry.feature.property_detail.PropertyDetailActivity
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.feature.splash.SplashActivity
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.registry.util.view.InfoAppCompatDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URLDecoder
import javax.inject.Inject

class MainActivity : BaseAppCompatActivity(),
    AppLifecycleHandler.AppStateChangedListener {

    @Inject
    lateinit var viewModel: MainViewModel

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var dialogController: DialogController

    @Inject
    lateinit var googleDriveSignIn: GoogleDriveSignIn

    @Inject
    internal lateinit var appLifecycleHandler: AppLifecycleHandler

    @Inject
    internal lateinit var connectivityHandler: ConnectivityHandler

    private lateinit var adapter: MainViewPagerAdapter

    private val handler = Handler()

    private val connectivityChangeListener =
        object : ConnectivityHandler.NetworkStateChangeListener {
            override fun onChange(connected: Boolean) {
                if (!connected) {
                    if (layoutNoNetwork.isVisible) return
                    layoutNoNetwork.visible(true)
                    handler.postDelayed({ layoutNoNetwork.gone(true) }, 2000)
                } else {
                    viewModel.cleanupBitmark()
                }
            }

        }

    override fun layoutRes(): Int = R.layout.activity_main

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isStorageEncryptionInactive()) {
            dialogController.alert(
                R.string.warning_your_phone_is_not_encrypted,
                R.string.encrypting_your_data
            ) {
                navigator.gotoSecuritySetting()
            }
        }
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
        } else if (intent?.data != null) {
            viewModel.prepareDeepLinkHandling()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val notificationBundle = intent?.getBundleExtra("notification")
        if (notificationBundle != null) {
            handleNotification(notificationBundle)
        } else {
            viewModel.prepareDeepLinkHandling()
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

    private fun handleDeepLinks(
        uri: Uri?,
        accountNumber: String,
        keyAlias: String
    ) {
        when (uri?.host) {
            "authorization" -> {
                val path = uri.path ?: return
                val data = path.removePrefix("/").split("/", limit = 2)
                if (data.size != 2) return
                val code = URLDecoder.decode(data[0], "UTF-8")
                val url = data[1]

                if (accountNumber.isNotEmpty() && keyAlias.isNotEmpty()) {
                    dialogController.confirm(
                        getString(R.string.authorization_required),
                        getString(R.string.requires_your_digital_signature_format).format(
                            url.toHost()
                        ), false,
                        getString(R.string.authorize),
                        {
                            authorize(url, code, accountNumber, keyAlias)
                        },
                        getString(R.string.cancel),
                        {})
                } else {
                    dialogController.alert(
                        R.string.authorization_required,
                        R.string.please_sign_in_or_create_bitmark_account
                    ) {
                        navigator.startActivityAsRoot(
                            RegisterContainerActivity::class.java,
                            RegisterContainerActivity.getBundle(uri)
                        )
                    }
                }
            }
        }
    }

    private fun authorize(
        url: String,
        code: String,
        accountNumber: String,
        keyAlias: String
    ) {
        loadAccount(
            accountNumber,
            keyAlias
        ) { account ->
            viewModel.authorize(
                accountNumber,
                url,
                code,
                account.keyPair
            )
        }
    }

    private fun loadAccount(
        accountNumber: String,
        keyAlias: String,
        action: (Account) -> Unit
    ) {
        val spec = KeyAuthenticationSpec.Builder(this).setKeyAlias(keyAlias)
            .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
            .setUsePossibleAlternativeAuthentication(true)
            .build()
        this.loadAccount(accountNumber,
            spec,
            dialogController,
            successAction = action,
            setupRequiredAction = { navigator.gotoSecuritySetting() })
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

        appLifecycleHandler.addAppStateChangedListener(this)

    }

    override fun deinitComponents() {
        appLifecycleHandler.removeAppStateChangedListener(this)
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
    }

    override fun onResume() {
        super.onResume()
        connectivityHandler.addNetworkStateChangeListener(
            connectivityChangeListener
        )
    }

    override fun onPause() {
        super.onPause()
        connectivityHandler.removeNetworkStateChangeListener(
            connectivityChangeListener
        )
    }

    override fun observe() {
        super.observe()
        viewModel.checkBitmarkSeenLiveData.observe(this, Observer { has ->
            if (has) {
                bottomNav.setNotification(
                    " ",
                    MainViewPagerAdapter.TAB_PROPERTIES
                )
            } else {
                bottomNav.setNotification(
                    "",
                    MainViewPagerAdapter.TAB_PROPERTIES
                )
            }
        })

        viewModel.checkActionRequiredLiveData.observe(this, Observer { ids ->
            if (ids.contains(ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION)) {
                bottomNav.setNotification(" ", MainViewPagerAdapter.TAB_ACCOUNT)
            } else if (!googleDriveSignIn.isSignedIn()) {
                navigator.anim(Navigator.BOTTOM_UP)
                    .startActivity(CloudServiceSignInActivity::class.java)
            } else {
                bottomNav.setNotification("", MainViewPagerAdapter.TAB_ACCOUNT)
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

        viewModel.prepareDeepLinkHandlingLiveData()
            .observe(this, Observer { res ->
                when {
                    res.isSuccess() -> {
                        val info = res.data() ?: return@Observer
                        val accountNumber = info.first
                        val keyAlias = info.second
                        handleDeepLinks(intent?.data, accountNumber, keyAlias)
                    }

                    res.isError() -> {
                        // ignore
                    }
                }
            })

        viewModel.authorizeLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val url = res.data() ?: ""
                    val infoDialog = InfoAppCompatDialog(
                        this,
                        getString(R.string.your_authorization_has_been_format).format(
                            url.toHost()
                        ),
                        getString(R.string.authorized)
                    )

                    dialogController.show(infoDialog)

                    handler.postDelayed(
                        {
                            dialogController.dismiss(infoDialog) {
                                dialogController.alert(
                                    "",
                                    getString(R.string.to_complete_process)
                                )
                            }
                        },
                        1500
                    )
                }

                res.isError() -> {
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_send_your_authorization
                    ) {
                        navigator.anim(
                            Navigator.RIGHT_LEFT
                        ).finishActivity()
                    }
                }
            }
        })

        viewModel.assetSyncProcessingErrorLiveData.observe(this, Observer { e ->
            if (e is UserRecoverableAuthIOException) {
                // user revoke app from google drive service
                // sign him in again
                val bundle =
                    CloudServiceSignInActivity.getBundle(signInIntent = e.intent)
                navigator.anim(Navigator.BOTTOM_UP)
                    .startActivity(
                        CloudServiceSignInActivity::class.java,
                        bundle
                    )
            }
        })
    }

    override fun onBackPressed() {
        val currentFragment = adapter.currentFragment as? BehaviorComponent
        if (currentFragment is PropertiesContainerFragment && !currentFragment.onBackPressed())
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
            PropertyDetailActivity::class.java,
            PropertyDetailActivity.getBundle(bitmark)
        )
    }

    private fun openTxHistory() {
        switchTab(MainViewPagerAdapter.TAB_TXS)
        //(adapter.currentFragment as? TransactionsFragment)?.openTxHistory()
    }

    override fun onForeground() {
        super.onForeground()
        viewModel.resumeSyncAsset()
    }

    override fun onBackground() {
        super.onBackground()
        viewModel.pauseSyncAsset()
    }
}
