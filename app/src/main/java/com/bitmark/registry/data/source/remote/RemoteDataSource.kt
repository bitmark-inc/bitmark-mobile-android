package com.bitmark.registry.data.source.remote

import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.service.*


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
    protected val registryApi: RegistryApi,
    protected val converter: Converter
) {
}