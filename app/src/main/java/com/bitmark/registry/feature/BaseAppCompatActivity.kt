package com.bitmark.registry.feature

import android.content.Intent
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

    private val lifecycleObserves = mutableListOf<ComponentLifecycleObserver>()

    protected fun addLifecycleObserver(observer: ComponentLifecycleObserver) {
        if (lifecycleObserves.contains(observer)) return
        lifecycleObserves.add(observer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleObserves.forEach { o -> o.onCreate() }
        if (viewModel() != null) {
            lifecycle.addObserver(viewModel()!!)
        }
        requestFeatures()
        setContentView(layoutRes())
        initComponents()
        observe()
    }

    override fun onStart() {
        super.onStart()
        lifecycleObserves.forEach { o -> o.onStart() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleObserves.forEach { o -> o.onResume() }
    }

    override fun onPause() {
        lifecycleObserves.forEach { o -> o.onPause() }
        super.onPause()
    }

    override fun onStop() {
        lifecycleObserves.forEach { o -> o.onStop() }
        super.onStop()
    }

    override fun onDestroy() {
        unobserve()
        deinitComponents()
        if (viewModel() != null) {
            lifecycle.removeObserver(viewModel()!!)
        }
        lifecycleObserves.forEach { o -> o.onDestroy() }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        lifecycleObserves.forEach { o ->
            o.onActivityResult(
                requestCode,
                resultCode,
                data
            )
        }
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

    protected open fun requestFeatures() {}

}