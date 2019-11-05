/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote

import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.middleware.RxErrorHandlingComposer
import com.bitmark.registry.data.source.remote.api.service.*

abstract class RemoteDataSource(
    protected val coreApi: CoreApi,
    protected val mobileServerApi: MobileServerApi,
    protected val fileCourierServerApi: FileCourierServerApi,
    protected val keyAccountServerApi: KeyAccountServerApi,
    protected val registryApi: RegistryApi,
    protected val converter: Converter,
    protected val rxErrorHandlingComposer: RxErrorHandlingComposer
) {
}