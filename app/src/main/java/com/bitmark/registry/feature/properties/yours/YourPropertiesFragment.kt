package com.bitmark.registry.feature.properties.yours

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bitmark.registry.AppLifecycleHandler
import com.bitmark.registry.R
import com.bitmark.registry.data.source.logging.Tracer
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.connectivity.ConnectivityHandler
import com.bitmark.registry.feature.issuance.selection.AssetSelectionFragment
import com.bitmark.registry.feature.music_claiming.MusicClaimingActivity
import com.bitmark.registry.feature.property_detail.PropertyDetailActivity
import com.bitmark.registry.util.EndlessScrollListener
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.visible
import com.bitmark.registry.util.view.SpeedyLinearLayoutManager
import kotlinx.android.synthetic.main.fragment_your_properties.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class YourPropertiesFragment : BaseSupportFragment(),
    AppLifecycleHandler.AppStateChangedListener {

    @Inject
    internal lateinit var viewModel: YourPropertiesViewModel

    @Inject
    internal lateinit var navigator: Navigator

    @Inject
    internal lateinit var appLifecycleHandler: AppLifecycleHandler

    @Inject
    internal lateinit var connectivityHandler: ConnectivityHandler

    private val adapter = YourPropertiesRecyclerViewAdapter()

    private lateinit var endlessScrollListener: EndlessScrollListener

    private val handler = Handler()

    private val connectivityChangeListener =
        object : ConnectivityHandler.NetworkStateChangeListener {
            override fun onChange(connected: Boolean) {
                if (connected) {
                    viewModel.fetchLatestBitmarks()
                }
            }

        }

    companion object {

        private const val TAG = "YourPropertiesFragment"

        fun newInstance() = YourPropertiesFragment()
    }

    override fun layoutRes(): Int = R.layout.fragment_your_properties

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handler.postDelayed({ viewModel.listBitmark() }, 250)
    }

    override fun initComponents() {
        super.initComponents()

        layoutSwipeRefresh.setColorSchemeResources(R.color.colorAccent)

        val layoutManager =
            SpeedyLinearLayoutManager(context!!, RecyclerView.VERTICAL, false)
        val itemDecoration =
            DividerItemDecoration(context, layoutManager.orientation)
        rvProperties.layoutManager = layoutManager
        rvProperties.addItemDecoration(itemDecoration)
        rvProperties.adapter = adapter
        rvProperties.setHasFixedSize(true)
        (rvProperties.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        adapter.setOnItemClickListener { bitmark ->
            viewModel.markSeen(bitmark.id)

            if (bitmark.isMusicClaiming()) {
                val bundle = MusicClaimingActivity.getBundle(bitmark)
                navigator.anim(BOTTOM_UP)
                    .startActivity(MusicClaimingActivity::class.java, bundle)
            } else {
                val bundle = PropertyDetailActivity.getBundle(bitmark)
                navigator.anim(RIGHT_LEFT)
                    .startActivity(
                        PropertyDetailActivity::class.java,
                        bundle
                    )
            }

        }

        endlessScrollListener =
            object : EndlessScrollListener(layoutManager) {
                override fun onLoadMore(
                    page: Int,
                    totalItemsCount: Int,
                    view: RecyclerView
                ) {
                    viewModel.listBitmark()
                }

            }
        rvProperties.addOnScrollListener(endlessScrollListener)

        btnCreateProperty.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).replaceChildFragment(
                R.id.layoutContainer,
                AssetSelectionFragment.newInstance()
            )
        }

        layoutSwipeRefresh.setOnRefreshListener {
            viewModel.refreshBitmarks()
        }

        appLifecycleHandler.addAppStateChangedListener(this)

        connectivityHandler.addNetworkStateChangeListener(
            connectivityChangeListener
        )

    }

    override fun deinitComponents() {
        handler.removeCallbacksAndMessages(null)
        connectivityHandler.removeNetworkStateChangeListener(
            connectivityChangeListener
        )
        appLifecycleHandler.removeAppStateChangedListener(this)
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()
        viewModel.listBitmarksLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    progressBar.gone()

                    val data = res.data()
                    if (!data.isNullOrEmpty()) {
                        hideEmptyView()
                        adapter.add(data)
                    } else if (adapter.isEmpty()) {
                        showEmptyView()
                    }
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "list bitmark failed: ${res.throwable() ?: "unknown"}"
                    )
                    progressBar.gone()
                }

                res.isLoading() -> {
                    progressBar.visible()
                }
            }
        })

        viewModel.refreshBitmarksLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    layoutSwipeRefresh.isRefreshing = false
                    adapter.clear()
                    endlessScrollListener.resetState()
                    viewModel.reset()
                    viewModel.listBitmark()
                }

                res.isError() -> {
                    // silence fetching, do nothing when error
                    Tracer.ERROR.log(
                        TAG,
                        "refresh bitmarks failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                    layoutSwipeRefresh.isRefreshing = false
                }
            }
        })

        viewModel.markSeenLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val id = res.data()
                    if (id != null)
                        adapter.markSeen(id)
                }

                res.isError() -> {
                    // ignore error
                    Tracer.ERROR.log(
                        TAG,
                        "mark bitmark seen failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                }
            }
        })

        viewModel.deletedBitmarkLiveData.observe(this, Observer { bitmarkId ->
            adapter.remove(bitmarkId)
            if (adapter.isEmpty()) {
                showEmptyView()
            }
        })

        viewModel.refreshAssetTypeLiveData.observe(this, Observer { bitmarks ->
            if (bitmarks.isEmpty()) return@Observer
            adapter.update(bitmarks)
        })

        viewModel.bitmarkSavedLiveData.observe(
            this,
            Observer { bitmarks ->
                adapter.update(bitmarks)
                if (adapter.isEmpty()) {
                    showEmptyView()
                } else {
                    hideEmptyView()
                }
            })

        viewModel.fetchLatestBitmarksLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val bitmarks = res.data() ?: return@Observer
                    adapter.update(bitmarks)
                }

                res.isError() -> {
                    // silence fetching so ignore error
                    Tracer.ERROR.log(
                        TAG,
                        "fetch latest bitmark failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                }
            }
        })
    }

    private fun showEmptyView() {
        tvWelcome.visible()
        tvIntroduce.visible()
        btnCreateProperty.visible()
        rvProperties.gone()
    }

    private fun hideEmptyView() {
        tvWelcome.gone()
        tvIntroduce.gone()
        btnCreateProperty.gone()
        rvProperties.visible()
    }

    override fun refresh() {
        super.refresh()
        rvProperties.smoothScrollToPosition(0)
    }

    override fun onForeground() {
        super.onForeground()
        viewModel.fetchLatestBitmarks()
    }
}