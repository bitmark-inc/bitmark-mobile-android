/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import dagger.android.support.DaggerFragment

abstract class BaseSupportFragment : DaggerFragment(), BehaviorComponent {

    protected var rootView: View? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (viewModel() != null) {
            lifecycle.addObserver(viewModel()!!)
        }
        observe()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(layoutRes(), container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initComponents()
    }

    override fun onDestroyView() {
        deinitComponents()
        if (null != viewModel())
            lifecycle.removeObserver(viewModel()!!)
        super.onDestroyView()
    }

    override fun onDetach() {
        unobserve()
        super.onDetach()
    }


    /**
     * Define the layout res id can be used to inflate [View]
     *
     * @return the layout res id
     */
    @LayoutRes
    protected abstract fun layoutRes(): Int

    /**
     * Define the [BaseViewModel] instance
     *
     * @return the [BaseViewModel] instance
     */
    protected abstract fun viewModel(): BaseViewModel?

    /**
     * Init [View] components here. Such as set adapter for [RecyclerView], set listener
     * or anything else
     */
    protected open fun initComponents() {}

    /**
     * Deinit [View] components here. Such as set adapter for [RecyclerView], remove listener
     * or anything else
     */
    protected open fun deinitComponents() {}

    /**
     * Observe data change from ViewModel
     */
    protected open fun observe() {}

    /**
     * Unobserve data change from ViewModel
     */
    protected open fun unobserve() {}
}