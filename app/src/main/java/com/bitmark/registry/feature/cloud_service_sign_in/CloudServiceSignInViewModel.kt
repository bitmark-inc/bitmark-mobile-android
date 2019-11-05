/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.cloud_service_sign_in

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.DateTimeUtil
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import java.util.*

class CloudServiceSignInViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) :
    BaseViewModel(lifecycle) {

    private val setCloudServiceRequiredLiveData = CompositeLiveData<Any>()

    internal fun setCloudServiceRequiredLiveData() =
        setCloudServiceRequiredLiveData.asLiveData()

    internal fun setCloudServiceRequired(required: Boolean) {
        setCloudServiceRequiredLiveData.add(
            rxLiveDataTransformer.completable(
                if (required) accountRepo.addActionRequired(
                    listOf(
                        ActionRequired(
                            ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION,
                            ActionRequired.Type.SECURITY_ALERT,
                            date = DateTimeUtil.dateToString(Date())
                        )
                    )
                )
                else {
                    accountRepo.deleteActionRequired(ActionRequired.Id.CLOUD_SERVICE_AUTHORIZATION)
                }
            )
        )
    }
}