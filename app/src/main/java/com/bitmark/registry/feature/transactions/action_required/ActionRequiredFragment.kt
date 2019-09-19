package com.bitmark.registry.feature.transactions.action_required

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.visible
import kotlinx.android.synthetic.main.fragment_action_required.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ActionRequiredFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() = ActionRequiredFragment()
    }

    @Inject
    lateinit var viewModel: ActionRequiredViewModel

    private lateinit var adapter: ActionRequiredAdapter

    override fun layoutRes(): Int = R.layout.fragment_action_required

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getActionRequired()
    }

    override fun initComponents() {
        super.initComponents()

        val layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val itemDecoration =
            DividerItemDecoration(context, layoutManager.orientation)
        adapter = ActionRequiredAdapter()
        rvActionRequired.layoutManager = layoutManager
        rvActionRequired.addItemDecoration(itemDecoration)
        rvActionRequired.setHasFixedSize(true)
        rvActionRequired.adapter = adapter

        adapter.setItemClickListener { action ->
            when (action.id) {
                ActionRequired.Id.RECOVERY_PHRASE -> {
                    val activity = this.activity as? MainActivity
                    activity?.openRecoveryPhraseWarning()
                }
            }
        }

    }

    override fun observe() {
        super.observe()

        viewModel.getActionRequiredLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val actions = res.data()!!
                    if (actions.isNotEmpty()) {
                        adapter.add(actions)
                        hideEmptyView()
                    } else if (adapter.isEmpty()) {
                        showEmptyView()
                    }
                }
            }
        })

        viewModel.actionDeletedLiveData.observe(this, Observer { actionId ->
            adapter.remove(actionId)

            if (adapter.isEmpty()) {
                showEmptyView()
            }
        })
    }

    private fun showEmptyView() {
        tvNoAction.visible()
        tvNoActionDes.visible()
    }

    private fun hideEmptyView() {
        tvNoAction.gone()
        tvNoActionDes.gone()
    }
}