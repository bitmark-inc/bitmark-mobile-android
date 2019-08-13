package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.source.local.api.DatabaseApi
import com.bitmark.registry.data.source.local.api.FileStorageApi
import com.bitmark.registry.data.source.local.api.SharedPrefApi
import io.reactivex.Completable
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-25
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AppLocalDataSource @Inject constructor(
    databaseApi: DatabaseApi,
    sharedPrefApi: SharedPrefApi, fileStorageApi: FileStorageApi
) : LocalDataSource(databaseApi, sharedPrefApi, fileStorageApi) {

    fun deleteDatabase() = databaseApi.rxCompletable { databaseGateway ->
        Completable.mergeArrayDelayError(
            databaseGateway.assetDao().delete(),
            databaseGateway.accountDao().delete(),
            databaseGateway.bitmarkDao().delete(),
            databaseGateway.blockDao().delete(),
            databaseGateway.transactionDao().delete(),
            databaseGateway.assetClaimingDao().delete()
        )
    }

    fun deleteSharePref() =
        sharedPrefApi.rxCompletable { sharePrefGateway -> sharePrefGateway.clear() }

    fun deleteQrCodeFile() =
        fileStorageApi.rxCompletable { fileStorageGateway ->
            val path = "%s/%s".format(
                fileStorageGateway.filesDir().absolutePath,
                "account/qrcode"
            )
            fileStorageGateway.delete(path)
        }

}