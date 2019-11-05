/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote

import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.middleware.RxErrorHandlingComposer
import com.bitmark.registry.data.source.remote.api.request.RegisterDeviceTokenRequest
import com.bitmark.registry.data.source.remote.api.service.*
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class AppRemoteDataSource @Inject constructor(
    coreApi: CoreApi,
    mobileServerApi: MobileServerApi,
    fileCourierServerApi: FileCourierServerApi,
    keyAccountServerApi: KeyAccountServerApi,
    registryApi: RegistryApi,
    converter: Converter,
    rxErrorHandlingComposer: RxErrorHandlingComposer
) : RemoteDataSource(
    coreApi,
    mobileServerApi,
    fileCourierServerApi,
    keyAccountServerApi,
    registryApi,
    converter,
    rxErrorHandlingComposer
) {

    fun deleteDeviceToken(deviceToken: String) =
        mobileServerApi.deleteDeviceToken(deviceToken).subscribeOn(Schedulers.io())

    fun registerDeviceToken(
        requester: String,
        timestamp: String,
        signature: String,
        token: String,
        intercomId: String?
    ) = mobileServerApi.registerDeviceToken(
        requester,
        timestamp,
        signature,
        RegisterDeviceTokenRequest(
            "android",
            token,
            BuildConfig.NOTIFICATION_CLIENT,
            intercomId
        )
    ).subscribeOn(Schedulers.io())

}