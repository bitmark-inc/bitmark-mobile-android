package com.bitmark.registry

import android.app.Application
import com.bitmark.registry.data.source.RepositoryModule
import com.bitmark.registry.data.source.remote.api.NetworkModule
import com.bitmark.registry.di.ActivityBuilderModule
import com.bitmark.registry.di.FragmentBuilderModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

@Component(
    modules = [AndroidSupportInjectionModule::class, AppModule::class,
        ActivityBuilderModule::class, FragmentBuilderModule::class,
        NetworkModule::class, RepositoryModule::class]
)
@Singleton
interface AppComponent : AndroidInjector<RegistryApplication> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(app: Application): Builder

        fun build(): AppComponent

    }
}