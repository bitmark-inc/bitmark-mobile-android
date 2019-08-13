package com.bitmark.registry.feature.properties.yours

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bitmark.registry.AppLifecycleHandler
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.issuance.selection.AssetSelectionFragment
import com.bitmark.registry.feature.music_claiming.MusicClaimingActivity
import com.bitmark.registry.feature.property_detail.PropertyDetailActivity
import com.bitmark.registry.util.EndlessScrollListener
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.visible
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

    private val adapter = YourPropertiesRecyclerViewAdapter()

    private lateinit var endlessScrollListener: EndlessScrollListener

    companion object {
        fun newInstance() = YourPropertiesFragment()
    }

    override fun layoutRes(): Int = R.layout.fragment_your_properties

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.listBitmark()
    }

    override fun initComponents() {
        super.initComponents()

        layoutSwipeRefresh.setColorSchemeResources(R.color.colorAccent)

        val layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
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

    }

    override fun deinitComponents() {
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
                }
            }
        })

        viewModel.deletedBitmarkLiveData.observe(this, Observer { bitmarkIds ->
            adapter.remove(bitmarkIds)
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