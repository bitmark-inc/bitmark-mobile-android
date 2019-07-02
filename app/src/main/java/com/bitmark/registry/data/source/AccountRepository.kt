package com.bitmark.registry.data.source

import com.bitmark.registry.data.source.local.AccountLocalDataSource
import com.bitmark.registry.data.source.remote.AccountRemoteDataSource


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AccountRepository(
    private val localDataSource: AccountLocalDataSource,
    private val remoteDataSource: AccountRemoteDataSource
) : AbsRepository() {
}