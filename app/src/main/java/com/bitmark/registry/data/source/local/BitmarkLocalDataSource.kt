package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.source.local.api.FileApi
import com.bitmark.registry.data.source.local.api.SharedPrefApi
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BitmarkLocalDataSource @Inject constructor(
    sharedPrefApi: SharedPrefApi, fileApi: FileApi
) : LocalDataSource(sharedPrefApi, fileApi) {
}