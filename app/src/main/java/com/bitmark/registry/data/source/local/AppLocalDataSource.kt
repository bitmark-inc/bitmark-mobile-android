/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local

import com.bitmark.registry.data.source.local.api.DatabaseApi
import com.bitmark.registry.data.source.local.api.FileStorageApi
import com.bitmark.registry.data.source.local.api.SharedPrefApi
import io.reactivex.Completable
import javax.inject.Inject

class AppLocalDataSource @Inject constructor(
    databaseApi: DatabaseApi,
    sharedPrefApi: SharedPrefApi, fileStorageApi: FileStorageApi
) : LocalDataSource(databaseApi, sharedPrefApi, fileStorageApi) {

    fun deleteDatabase() = databaseApi.rxCompletable { databaseGateway ->
        Completable.mergeArrayDelayError(
            databaseGateway.assetDao().deleteR(),
            databaseGateway.assetDao().deleteL(),
            databaseGateway.accountDao().delete(),
            databaseGateway.bitmarkDao().deleteR(),
            databaseGateway.bitmarkDao().deleteL(),
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

    fun deleteFiles(path: String) =
        fileStorageApi.rxCompletable { fileStorageGateway ->
            fileStorageGateway.delete(path)
        }

}