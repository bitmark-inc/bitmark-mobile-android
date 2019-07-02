package com.bitmark.registry.data.source.remote

import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.service.CoreApi
import com.bitmark.registry.data.source.remote.api.service.FileCourierServerApi
import com.bitmark.registry.data.source.remote.api.service.MobileServerApi
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BitmarkRemoteDataSource @Inject constructor(
    coreApi: CoreApi, mobileServerApi: MobileServerApi,
    fileCourierServerApi: FileCourierServerApi, converter: Converter
) : RemoteDataSource(
    coreApi, mobileServerApi, fileCourierServerApi, converter
) {
}