package com.bitmark.registry.di

import android.os.Bundle
import com.bitmark.sdk.authentication.StatefulActivity
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

/**
 * @author Hieu Pham
 * @since 7/7/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
abstract class DaggerAppCompatActivity : StatefulActivity(), HasAndroidInjector {

  @Inject
  lateinit var androidInjector: DispatchingAndroidInjector<Any>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AndroidInjection.inject(this)
  }

  override fun androidInjector(): AndroidInjector<Any> = androidInjector
}