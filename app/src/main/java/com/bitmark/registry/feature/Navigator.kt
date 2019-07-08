package com.bitmark.registry.feature

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
class Navigator<T>(host: T) {

    companion object {
        const val BOTTOM_UP = 0x01
        const val RIGHT_LEFT = 0x02
    }

    private var fragment: Fragment? = null
    private var activity: FragmentActivity? = null
    private var anim: Int = 0x00

    init {
        if (host is FragmentActivity) activity = host
        else if (host is Fragment) {
            fragment = host
            activity = host.activity
        }
    }

    fun anim(anim: Int): Navigator<T> {
        this.anim = anim
        return this
    }

    fun replaceFragment(
        @IdRes container: Int, fragment: Fragment,
        addToBackStack: Boolean
    ) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transactionAnim(transaction)
        transaction?.replace(container, fragment)
        if (addToBackStack) transaction?.addToBackStack(null)
        transaction?.commitAllowingStateLoss()
    }

    fun popFragment() {
        val fragManager = activity?.supportFragmentManager
        fragManager?.popBackStackImmediate()
    }

    fun addFragment(@IdRes container: Int, fragment: Fragment) {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.add(container, fragment)
            ?.commitAllowingStateLoss()
    }

    fun startActivity(intent: Intent) {
        activity?.startActivity(intent)
        transactionAnim(activity)
    }

    fun startActivityAsRoot(intent: Intent) {
        activity?.finishAffinity()
        activity?.startActivity(intent)
        transactionAnim(activity)
    }

    fun startActivity(clazz: Class<*>, bundle: Bundle? = null) {
        val intent = Intent(activity, clazz)
        if (null != bundle) intent.putExtras(bundle)
        activity?.startActivity(intent)
        transactionAnim(activity)
    }

    fun startActivityAsRoot(clazz: Class<*>, bundle: Bundle? = null) {
        val intent = Intent(activity, clazz)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (null != bundle) intent.putExtras(bundle)
        activity?.finishAffinity()
        activity?.startActivity(intent)
        transactionAnim(activity)
    }

    fun finishActivity() {
        activity?.finish()
    }

    private fun transactionAnim(activity: FragmentActivity?) {
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