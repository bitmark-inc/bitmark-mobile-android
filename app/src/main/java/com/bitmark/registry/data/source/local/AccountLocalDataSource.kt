/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local

import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.bitmark.registry.data.model.entity.AccountData
import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.data.source.local.api.DatabaseApi
import com.bitmark.registry.data.source.local.api.FileStorageApi
import com.bitmark.registry.data.source.local.api.SharedPrefApi
import com.bitmark.registry.data.source.local.event.ActionRequiredAddedListener
import com.bitmark.registry.data.source.local.event.ActionRequiredDeletedListener
import com.bitmark.registry.util.extension.append
import com.bitmark.registry.util.extension.fromJson
import com.bitmark.registry.util.extension.toJson
import com.google.gson.GsonBuilder
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class AccountLocalDataSource @Inject constructor(
    databaseApi: DatabaseApi,
    sharedPrefApi: SharedPrefApi, fileStorageApi: FileStorageApi
) : LocalDataSource(databaseApi, sharedPrefApi, fileStorageApi) {

    private var actionRequiredDeleteListener: ActionRequiredDeletedListener? =
        null

    private var actionRequiredAddedListener: ActionRequiredAddedListener? = null

    fun setActionRequiredDeletedListener(listener: ActionRequiredDeletedListener) {
        this.actionRequiredDeleteListener = listener
    }

    fun setActionRequiredAddedListener(listener: ActionRequiredAddedListener) {
        this.actionRequiredAddedListener = listener
    }

    fun saveAccountInfo(
        accountNumber: String,
        authRequired: Boolean,
        keyAlias: String
    ): Completable {
        return sharedPrefApi.rxCompletable { sharePrefGateway ->
            sharePrefGateway.put(SharedPrefApi.ACCOUNT_NUMBER, accountNumber)
            sharePrefGateway.put(SharedPrefApi.AUTH_REQUIRED, authRequired)
            sharePrefGateway.put(SharedPrefApi.KEY_ALIAS, keyAlias)
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

    fun getKeyAlias() = sharedPrefApi.rxSingle { sharePrefGateway ->
        var keyAlias =
            sharePrefGateway.get(SharedPrefApi.KEY_ALIAS, String::class)
        if (keyAlias.isEmpty()) keyAlias =
            sharePrefGateway.get(SharedPrefApi.ACCOUNT_NUMBER, String::class)
        keyAlias
    }

    fun checkAccessRemoved() = sharedPrefApi.rxSingle { sharePrefGateway ->
        sharePrefGateway.get(SharedPrefApi.ACCESS_REMOVED, Boolean::class)
    }

    fun removeAccess() = sharedPrefApi.rxCompletable { sharePrefGateway ->
        sharePrefGateway.put(SharedPrefApi.ACCESS_REMOVED, true)
    }

    fun saveEncPubKey(accountNumber: String, encPubKey: String): Completable =
        databaseApi.rxCompletable { db ->
            db.accountDao().saveEncPubKey(
                AccountData(
                    accountNumber,
                    encPubKey
                )
            )
        }

    fun getEncPubKey(accountNumber: String): Single<ByteArray> =
        databaseApi.rxSingle { db ->
            db.accountDao().getEncPubKey(accountNumber)
        }.map { hexKey -> HEX.decode(hexKey) }

    fun addActionRequired(actions: List<ActionRequired>) =
        getActionRequired().flatMapCompletable { existingActions ->
            val mutableExistingActions = existingActions.toMutableList()
            // remove duplicate actions
            actions.forEach { action ->
                val existingPos =
                    mutableExistingActions.indexOfFirst { a -> action.id == a.id }
                if (existingPos != -1) {
                    mutableExistingActions.removeAt(existingPos)
                }
            }

            val persistActions =
                if (mutableExistingActions.isEmpty()) actions else mutableListOf<ActionRequired>().append(
                    mutableExistingActions,
                    actions
                )

            sharedPrefApi.rxCompletable { sharePrefGateway ->
                sharePrefGateway.put(
                    SharedPrefApi.ACTION_REQUIRED,
                    gson().toJson<List<ActionRequired>>(persistActions)
                )
            }.doOnComplete {
                actionRequiredAddedListener?.onAdded(
                    persistActions.map { a -> a.id })
            }
        }


    fun getActionRequired(): Single<List<ActionRequired>> =
        sharedPrefApi.rxSingle { sharePrefGateway ->
            gson().fromJson<List<ActionRequired>>(
                sharePrefGateway.get(
                    SharedPrefApi.ACTION_REQUIRED,
                    String::class
                )
            ) ?: listOf()
        }

    fun deleteActionRequired(actionId: ActionRequired.Id) =
        getActionRequired().flatMapCompletable { existingActions ->
            val persistActions =
                existingActions.filterNot { a -> a.id == actionId }
            sharedPrefApi.rxCompletable { sharePrefGateway ->
                sharePrefGateway.put(
                    SharedPrefApi.ACTION_REQUIRED,
                    persistActions
                )
            }.doOnComplete {
                actionRequiredDeleteListener?.onDeleted(
                    actionId
                )
            }
        }

    private fun gson() = GsonBuilder().excludeFieldsWithoutExposeAnnotation()
        .setLenient()
        .create()
}