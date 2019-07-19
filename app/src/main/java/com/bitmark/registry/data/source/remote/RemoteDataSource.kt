package com.bitmark.registry.data.source.remote

import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.service.CoreApi
import com.bitmark.registry.data.source.remote.api.service.FileCourierServerApi
import com.bitmark.registry.data.source.remote.api.service.KeyAccountServerApi
import com.bitmark.registry.data.source.remote.api.service.MobileServerApi


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
abstract class RemoteDataSource(
    protected val coreApi: CoreApi,
    protected val mobileServerApi: MobileServerApi,
    protected val fileCourierServerApi: FileCourierServerApi,
    protected val keyAccountServerApi: KeyAccountServerApi,
    protected val converter: Converter
) {
}