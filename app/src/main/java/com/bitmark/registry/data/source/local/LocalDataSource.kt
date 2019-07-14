package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.source.local.api.DatabaseApi
import com.bitmark.registry.data.source.local.api.FileStorageApi
import com.bitmark.registry.data.source.local.api.SharedPrefApi


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
abstract class LocalDataSource(
    protected val databaseApi: DatabaseApi,
    protected val sharedPrefApi: SharedPrefApi,
    protected val fileStorageApi: FileStorageApi
)