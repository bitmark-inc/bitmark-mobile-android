package com.bitmark.registry.feature.main.transactions.action_required

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import com.bitmark.registry.util.modelview.ActionRequiredModelView


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ActionRequiredViewModel(
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) : BaseViewModel() {

    private val getActionRequiredLiveData =
        CompositeLiveData<List<ActionRequiredModelView>>()

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

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}