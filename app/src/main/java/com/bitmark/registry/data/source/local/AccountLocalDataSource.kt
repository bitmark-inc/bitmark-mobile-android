package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.source.local.api.DatabaseApi
import com.bitmark.registry.data.source.local.api.FileStorageApi
import com.bitmark.registry.data.source.local.api.SharedPrefApi
import io.reactivex.Single
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AccountLocalDataSource @Inject constructor(
    databaseApi: DatabaseApi,
    sharedPrefApi: SharedPrefApi, fileStorageApi: FileStorageApi
) : LocalDataSource(databaseApi, sharedPrefApi, fileStorageApi) {

    fun saveAccountInfo(
        accountNumber: String,
        authRequired: Boolean
    ): Single<Pair<String, Boolean>> {
        return sharedPrefApi.rxSingle { sharePrefGateway ->
            sharePrefGateway.put(SharedPrefApi.ACCOUNT_NUMBER, accountNumber)
            sharePrefGateway.put(SharedPrefApi.AUTH_REQUIRED, authRequired)
            Pair(accountNumber, authRequired)
        }
    }

    fun getAccountInfo(): Single<Pair<String, Boolean>> {
        return sharedPrefApi.rxSingle { sharePrefGateway ->
            val accountNumber = sharePrefGateway.get(
                SharedPrefApi.ACCOUNT_NUMBER, String::class
            )
            val authRequired = sharePrefGateway.get(
                SharedPrefApi.AUTH_REQUIRED,
                Boolean::class
            )
            Pair(accountNumber, authRequired)
        }
    }
}