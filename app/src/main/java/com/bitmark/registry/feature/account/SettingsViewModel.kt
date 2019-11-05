/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.account

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.BufferedLiveData
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.android.schedulers.AndroidSchedulers

class SettingsViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) :
    BaseViewModel(lifecycle) {

    private val getAccountNumberLiveData = CompositeLiveData<String>()

    internal val checkCloudServiceRequiredLiveData =
        BufferedLiveData<Boolean>(lifecycle)

    internal val cloudServiceRequiredChangedLiveData =
        MutableLiveData<Boolean>()

    internal fun getAccountNumberLiveData() =
        getAccountNumberLiveData.asLiveData()

    internal fun getAccountNumber() {
        getAccountNumberLiveData.add(rxLiveDataTransformer.single(accountRepo.getAccountInfo().map { p -> p.first }))
    }

    internal fun checkCloudServiceRequired() = subscribe(
        accountRepo.getActionRequired().observeOn(AndroidSchedulers.mainThread()).subscribe { actions, e ->
            checkCloudServiceRequiredLiveData.setValue(
                actions.map { a -> a.id }.contains(
                    ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION
                ) || e != null
            )
        })

    override fun onStart() {
        super.onStart()
        realtimeBus.actionRequiredAddedPublisher.subscribe(this) { actionIds ->
            if (actionIds.contains(ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION)) {
                cloudServiceRequiredChangedLiveData.set(true)
            }
        }

        realtimeBus.actionRequiredDeletedPublisher.subscribe(this) { actionId ->
            if (actionId == ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION) {
                cloudServiceRequiredChangedLiveData.set(false)
            }
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}