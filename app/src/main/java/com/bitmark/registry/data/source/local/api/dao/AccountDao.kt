/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitmark.registry.data.model.entity.AccountData
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveEncPubKey(accountData: AccountData): Completable

    @Query("SELECT (enc_pub_key) FROM Account WHERE account_number = :accountNumber")
    abstract fun getEncPubKey(accountNumber: String): Single<String>

    @Query("DELETE FROM Account")
    abstract fun delete(): Completable
}