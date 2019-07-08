package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.source.local.api.FileApi
import com.bitmark.registry.data.source.local.api.SharedPrefApi


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
abstract class LocalDataSource(
    protected val sharedPrefApi: SharedPrefApi, protected val fileApi: FileApi
)