package com.bitmark.registry.data.source.remote

import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.service.CoreApi
import com.bitmark.registry.data.source.remote.api.service.FileCourierServerApi
import com.bitmark.registry.data.source.remote.api.service.KeyAccountServerApi
import com.bitmark.registry.data.source.remote.api.service.MobileServerApi
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-25
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AppRemoteDataSource @Inject constructor(
    coreApi: CoreApi,
    mobileServerApi: MobileServerApi,
    fileCourierServerApi: FileCourierServerApi,
    keyAccountServerApi: KeyAccountServerApi,
    converter: Converter
) : RemoteDataSource(
    coreApi,
    mobileServerApi,
    fileCourierServerApi,
    keyAccountServerApi,
    converter
) {

    fun deleteDeviceToken(deviceToken: String) =
        mobileServerApi.deleteDeviceToken(deviceToken).subscribeOn(Schedulers.io())

}