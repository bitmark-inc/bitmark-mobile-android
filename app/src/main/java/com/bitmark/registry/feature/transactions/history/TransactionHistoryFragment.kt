package com.bitmark.registry.feature.transactions.history

import android.os.Handler
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bitmark.registry.AppLifecycleHandler
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.*
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.EndlessScrollListener
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.visible
import com.bitmark.registry.util.view.SpeedyLinearLayoutManager
import kotlinx.android.synthetic.main.fragment_transaction_history.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class TransactionHistoryFragment : BaseSupportFragment(),
    AppLifecycleHandler.AppStateChangedListener {

    @Inject
    internal lateinit var viewModel: TransactionHistoryViewModel

    @Inject
    internal lateinit var navigator: Navigator

    @Inject
    internal lateinit var dialogController: DialogController

    @Inject
    internal lateinit var appLifecycleHandler: AppLifecycleHandler

    private val adapter = TransactionHistoryAdapter()

    private lateinit var endlessScrollListener: EndlessScrollListener

    private var visibled = false

    private val handler = Handler()

    companion object {
        fun newInstance() = TransactionHistoryFragment()
    }

    override fun layoutRes(): Int = R.layout.fragment_transaction_history

    override fun viewModel(): BaseViewModel? = viewModel

    override fun initComponents() {
        super.initComponents()

        layoutSwipeRefresh.setColorSchemeResources(R.color.colorAccent)

        val layoutManager =
            SpeedyLinearLayoutManager(context!!, RecyclerView.VERTICAL, false)
        val itemDecoration =
            DividerItemDecoration(context, layoutManager.orientation)
        rvTxs.layoutManager = layoutManager
        rvTxs.addItemDecoration(itemDecoration)
        rvTxs.adapter = adapter
        rvTxs.setHasFixedSize(true)
        (rvTxs.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        adapter.setItemClickListener { item ->
            if (item.isPending() || item.isAssetClaiming()) return@setItemClickListener

            val url = "%s/transaction/%s".format(
                BuildConfig.REGISTRY_WEBSITE,
                item.id
            )
            val bundle =
                WebViewActivity.getBundle(url, getString(R.string.registry))
            navigator.anim(RIGHT_LEFT)
                .startActivity(WebViewActivity::class.java, bundle)
        }

        endlessScrollListener =
            object : EndlessScrollListener(layoutManager) {
                override fun onLoadMore(
                    page: Int,
                    totalItemsCount: Int,
                    view: RecyclerView
                ) {
                    viewModel.listTxs()
                }

            }
        rvTxs.addOnScrollListener(endlessScrollListener)

        layoutSwipeRefresh.setOnRefreshListener {
            viewModel.refreshTxs()
        }

        appLifecycleHandler.addAppStateChangedListener(this)
    }

    override fun deinitComponents() {
        visibled = false
        handler.removeCallbacksAndMessages(null)
        appLifecycleHandler.removeAppStateChangedListener(this)
        dialogController.dismiss()
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()
        viewModel.listTxsLiveData().observe(this, Observer { res ->
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
                    if (adapter.isEmpty()) {
                        showEmptyView()
                    }
                }

                res.isLoading() -> {
                    progressBar.visible()
                }
            }
        })

        viewModel.refreshTxsLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    layoutSwipeRefresh.isRefreshing = false
                    adapter.clear()
                    endlessScrollListener.resetState()
                    viewModel.reset()
                    viewModel.listTxs()
                }

                res.isError() -> {
                    layoutSwipeRefresh.isRefreshing = false
                    progressBar.gone()
                }
            }
        })

        viewModel.txsSavedLiveData.observe(this, Observer { txs ->
            adapter.update(txs)
            if (adapter.isEmpty()) {
                showEmptyView()
            } else {
                hideEmptyView()
            }
        })

        viewModel.fetchLatestTxsLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val txs = res.data() ?: return@Observer
                    adapter.update(txs)
                }

                res.isError() -> {
                    // silence fetching so ignore error
                }
            }
        })
    }

    private fun showEmptyView() {
        tvNoTxs.visible()
        tvNoTxsDes.visible()
    }

    private fun hideEmptyView() {
        tvNoTxs.gone()
        tvNoTxsDes.gone()
    }

    override fun refresh() {
        super.refresh()
        rvTxs.smoothScrollToPosition(0)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && !visibled) {
            // bit delay for better ux
            handler.postDelayed({
                visibled = true
                viewModel.listTxs()
            }, 200)
        }
    }

    override fun onForeground() {
        super.onForeground()
        viewModel.fetchLatestTxs()
    }
}