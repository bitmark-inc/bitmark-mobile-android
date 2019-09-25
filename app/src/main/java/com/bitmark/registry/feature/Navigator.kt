package com.bitmark.registry.feature

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.bitmark.registry.R


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class Navigator(host: Any) {

    companion object {
        const val NONE = 0x00
        const val BOTTOM_UP = 0x01
        const val RIGHT_LEFT = 0x02
    }

    private var fragment: Fragment? = null
    private var activity: FragmentActivity? = null
    private var anim: Int = NONE

    init {
        if (host is FragmentActivity) activity = host
        else if (host is Fragment) {
            fragment = host
            activity = host.activity
        }
    }

    fun anim(anim: Int): Navigator {
        this.anim = anim
        return this
    }

    fun replaceFragment(
        @IdRes container: Int, fragment: Fragment,
        addToBackStack: Boolean = true
    ) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transactionAnim(transaction)
        transaction?.replace(
            container,
            fragment,
            fragment::class.java.simpleName
        )
        if (addToBackStack) transaction?.addToBackStack(null)
        transaction?.commitAllowingStateLoss()
    }

    fun replaceChildFragment(
        @IdRes container: Int, fragment: Fragment,
        addToBackStack: Boolean = true
    ) {
        val transaction =
            this.fragment?.childFragmentManager?.beginTransaction()
        transactionAnim(transaction)
        transaction?.replace(
            container,
            fragment,
            fragment::class.java.simpleName
        )
        if (addToBackStack) transaction?.addToBackStack(null)
        transaction?.commitAllowingStateLoss()
    }

    fun addFragment(
        @IdRes container: Int, fragment: Fragment,
        addToBackStack: Boolean = true
    ) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transactionAnim(transaction)
        if (addToBackStack) transaction?.addToBackStack(null)
        transaction?.add(container, fragment, fragment::class.java.simpleName)
        transaction?.commitAllowingStateLoss()
    }

    fun popFragment() =
        activity?.supportFragmentManager?.popBackStackImmediate()

    fun popChildFragment() =
        fragment?.childFragmentManager?.popBackStackImmediate()

    fun popChildFragmentToRoot(): Boolean {
        val fragmentManager = fragment?.childFragmentManager
        val count = fragmentManager?.backStackEntryCount ?: return false
        var popped = false

        for (i in 0 until count) {
            popped = fragmentManager.popBackStackImmediate() || popped
        }

        return popped
    }

    fun startActivity(intent: Intent) {
        try {
            activity?.startActivity(intent)
            startTransactionAnim(activity)
        } catch (ignore: ActivityNotFoundException) {
        }
    }

    fun startActivityAsRoot(intent: Intent) {
        activity?.finishAffinity()
        activity?.startActivity(intent)
        startTransactionAnim(activity)
    }

    fun startActivity(clazz: Class<*>, bundle: Bundle? = null) {
        val intent = Intent(activity, clazz)
        if (null != bundle) intent.putExtras(bundle)
        activity?.startActivity(intent)
        startTransactionAnim(activity)
    }

    fun startActivityForResult(
        clazz: Class<*>,
        requestCode: Int,
        bundle: Bundle? = null
    ) {
        val intent = Intent(activity, clazz)
        if (null != bundle) intent.putExtras(bundle)
        if (fragment != null) {
            fragment?.startActivityForResult(intent, requestCode)
        } else {
            activity?.startActivityForResult(intent, requestCode)
        }
        startTransactionAnim(activity)
    }

    fun startActivityForResult(intent: Intent, requestCode: Int) {
        if (fragment != null) {
            fragment?.startActivityForResult(intent, requestCode)
        } else {
            activity?.startActivityForResult(intent, requestCode)
        }
        startTransactionAnim(activity)
    }

    fun startActivityAsRoot(clazz: Class<*>, bundle: Bundle? = null) {
        val intent = Intent(activity, clazz)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (null != bundle) intent.putExtras(bundle)
        activity?.finishAffinity()
        activity?.startActivity(intent)
        startTransactionAnim(activity)
    }

    fun finishActivity() {
        activity?.finish()
        finishTransactionAnim(activity)
    }

    private fun startTransactionAnim(activity: FragmentActivity?) {
        when (anim) {
            BOTTOM_UP -> activity?.overridePendingTransition(
                R.anim.slide_bottom_in,
                0
            )
            RIGHT_LEFT -> activity?.overridePendingTransition(
                R.anim.slide_right_in,
                R.anim.slide_left_out
            )
        }
    }

    private fun finishTransactionAnim(activity: FragmentActivity?) {
        when (anim) {
            BOTTOM_UP -> activity?.overridePendingTransition(
                0,
                R.anim.slide_bottom_out
            )
            RIGHT_LEFT -> activity?.overridePendingTransition(
                R.anim.slide_left_in,
                R.anim.slide_right_out
            )
        }
    }

    private fun transactionAnim(transaction: FragmentTransaction?) {
        if (null == transaction) return
        when (anim) {
            BOTTOM_UP -> transaction.setCustomAnimations(
                R.anim.slide_bottom_in,
                0
            )
            RIGHT_LEFT -> transaction.setCustomAnimations(
                R.anim.slide_right_in,
                R.anim.slide_left_out,
                R.anim.slide_left_in, R.anim.slide_right_out
            )
        }
    }
}