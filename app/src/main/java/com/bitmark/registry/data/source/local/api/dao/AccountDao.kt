package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitmark.registry.data.model.AccountData
import io.reactivex.Completable
import io.reactivex.Single


/**
 * @author Hieu Pham
 * @since 2019-07-18
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Dao
abstract class AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveEncPubKey(accountData: AccountData): Completable

    @Query("SELECT (enc_pub_key) FROM Account WHERE account_number = :accountNumber")
    abstract fun getEncPubKey(accountNumber: String): Single<String>

    @Query("DELETE FROM Account")
    abstract fun delete(): Completable
}