/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.model.entity.TransactionDataR
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class TransactionDao {

    @Insert(onConflict = REPLACE)
    abstract fun saveR(tx: TransactionDataR): Completable

    @Transaction
    @Query("SELECT * FROM TransactionR WHERE bitmark_id = :bitmarkId AND status IN (:status) ORDER BY `offset` DESC LIMIT :limit")
    abstract fun listByBitmarkIdStatusLimitDesc(
        bitmarkId: String,
        status: Array<TransactionData.Status>,
        limit: Int
    ): Single<List<TransactionData>>

    @Query("DELETE FROM TransactionR WHERE bitmark_id = :bitmarkId ")
    abstract fun deleteByBitmarkId(bitmarkId: String): Completable

    @Query("DELETE FROM TransactionR WHERE bitmark_id = :bitmarkId AND owner != :who AND previous_owner != :who")
    abstract fun deleteIrrelevantByBitmarkId(
        who: String,
        bitmarkId: String
    ): Completable

    @Query("DELETE FROM TransactionR WHERE bitmark_id IN (:bitmarkIds) ")
    abstract fun deleteByBitmarkIds(bitmarkIds: List<String>): Completable

    @Transaction
    @Query("SELECT * FROM TransactionR WHERE (owner = :owner OR previous_owner = :previousOwner) AND status IN (:status) AND `offset` <= :offset ORDER BY `offset` DESC LIMIT :limit")
    abstract fun listByOwnerOffsetStatusLimitDesc(
        owner: String,
        previousOwner: String,
        offset: Long,
        status: Array<TransactionData.Status>,
        limit: Int
    ): Single<List<TransactionData>>

    @Query("SELECT MAX(`offset`) FROM TransactionR WHERE owner = :who OR previous_owner = :who")
    abstract fun maxRelevantOffset(who: String): Single<Long>

    @Query("SELECT MIN(`offset`) FROM TransactionR WHERE owner = :who OR previous_owner = :who")
    abstract fun minRelevantOffset(who: String): Single<Long>

    @Query("SELECT MIN(`offset`) FROM TransactionR WHERE (owner = :who OR previous_owner = :who) AND status IN (:status)")
    abstract fun minRelevantOffset(
        who: String,
        status: Array<TransactionData.Status>
    ): Single<Long>

    @Transaction
    @Query("SELECT * FROM TransactionR WHERE (owner = :who OR previous_owner = :who) AND status == :status ORDER BY `offset` DESC")
    abstract fun listRelevantByStatusDesc(
        who: String,
        status: TransactionData.Status
    ): Single<List<TransactionData>>

    @Query("DELETE FROM TransactionR")
    abstract fun delete(): Completable

    @Transaction
    @Query("SELECT * FROM TransactionR WHERE id == :id")
    abstract fun getById(id: String): Single<TransactionData>

}