package com.bitmark.registry.feature

import android.os.Bundle
import androidx.annotation.LayoutRes
import com.bitmark.registry.di.DaggerAppCompatActivity

/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
abstract class BaseAppCompatActivity : DaggerAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (viewModel() != null) {
            lifecycle.addObserver(viewModel()!!)
        }
        setContentView(layoutRes())
        initComponents()
        observe()
    }

    override fun onDestroy() {
        unobserve()
        deinitComponents()
        if (viewModel() != null) {
            lifecycle.removeObserver(viewModel()!!)
        }
        super.onDestroy()
    }

    /**
     * Define the layout res id can be used to [Activity.setContentView]
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