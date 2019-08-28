package com.bitmark.registry.feature.issuance.selection

import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-30
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class AssetSelectionModule {

    @Provides
    @FragmentScope
    fun provideNavigator(fragment: AssetSelectionFragment) =
        Navigator(fragment.parentFragment!!)

    @Provides
    @FragmentScope
    fun provideDialogController(fragment: AssetSelectionFragment) =
        DialogController(fragment.activity!!)

    @Provides
    @FragmentScope
    fun provideViewModel(
        fragment: AssetSelectionFragment,
        bitmarkRepo: BitmarkRepository,
        appRepo: AppRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        realtimeBus: RealtimeBus
    ) = AssetSelectionViewModel(
        fragment.lifecycle,
        bitmarkRepo,
        appRepo,
        rxLiveDataTransformer,
        realtimeBus
    )
}