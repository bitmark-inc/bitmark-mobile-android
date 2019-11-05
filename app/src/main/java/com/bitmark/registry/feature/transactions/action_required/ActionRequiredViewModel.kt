/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.transactions.action_required

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.ActionRequiredModelView

class ActionRequiredViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel(lifecycle) {

    private val getActionRequiredLiveData =
        CompositeLiveData<List<ActionRequiredModelView>>()

    internal val actionDeletedLiveData = MutableLiveData<ActionRequired.Id>()

    internal fun getActionRequiredLiveData() =
        getActionRequiredLiveData.asLiveData()

    internal fun getActionRequired() {
        getActionRequiredLiveData.add(
            rxLiveDataTransformer.single(
                getActionRequiredStream()
            )
        )
    }

    private fun getActionRequiredStream() =
        accountRepo.getActionRequired().map { actions ->
            if (actions.isEmpty()) listOf()
            else
                actions.map { a ->
                    ActionRequiredModelView(
                        a.id,
                        a.type,
                        a.titleStringResName,
                        a.desStringResName,
                        a.date
                    )
                }
        }

    override fun onCreate() {
        super.onCreate()
        realtimeBus.actionRequiredDeletedPublisher.subscribe(this) { actionId ->
            actionDeletedLiveData.set(
                actionId
            )
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}