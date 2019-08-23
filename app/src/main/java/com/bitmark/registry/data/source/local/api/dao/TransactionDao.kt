package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.bitmark.registry.data.model.TransactionData
import io.reactivex.Completable
import io.reactivex.Single


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Dao
abstract class TransactionDao {

    @Insert(onConflict = REPLACE)
    abstract fun save(txs: List<TransactionData>): Completable

    @Query("SELECT * FROM `Transaction` WHERE bitmark_id = :bitmarkId AND status IN (:status) ORDER BY `offset` DESC LIMIT :limit")
    abstract fun listByBitmarkIdStatusLimitDesc(
        bitmarkId: String,
        status: Array<TransactionData.Status>,
        limit: Int
    ): Single<List<TransactionData>>

    @Query("DELETE FROM `Transaction` WHERE bitmark_id = :bitmarkId ")
    abstract fun deleteByBitmarkId(bitmarkId: String): Completable

    @Query("DELETE FROM `Transaction` WHERE bitmark_id = :bitmarkId AND owner != :who AND previous_owner != :who")
    abstract fun deleteIrrelevantByBitmarkId(
        who: String,
        bitmarkId: String
    ): Completable

    @Query("DELETE FROM `Transaction` WHERE bitmark_id IN (:bitmarkIds) ")
    abstract fun deleteByBitmarkIds(bitmarkIds: List<String>): Completable

    @Query("SELECT * FROM `Transaction` WHERE (owner = :owner OR previous_owner = :previousOwner) AND status IN (:status) AND `offset` <= :offset ORDER BY `offset` DESC LIMIT :limit")
    abstract fun listByOwnerOffsetStatusLimitDesc(
        owner: String,
        previousOwner: String,
        offset: Long,
        status: Array<TransactionData.Status>,
        limit: Int
    ): Single<List<TransactionData>>

    @Query("SELECT MAX(`offset`) FROM `Transaction` WHERE owner = :who OR previous_owner = :who")
    abstract fun maxRelevantOffset(who: String): Single<Long>

    @Query("SELECT MIN(`offset`) FROM `Transaction` WHERE owner = :who OR previous_owner = :who")
    abstract fun minRelevantOffset(who: String): Single<Long>

    @Query("SELECT MIN(`offset`) FROM `Transaction` WHERE (owner = :who OR previous_owner = :who) AND status IN (:status)")
    abstract fun minRelevantOffset(
        who: String,
        status: Array<TransactionData.Status>
    ): Single<Long>

    @Query("SELECT * FROM `Transaction` WHERE (owner = :who OR previous_owner = :who) AND status == :status ORDER BY `offset` DESC")
    abstract fun listRelevantByStatusDesc(
        who: String,
        status: TransactionData.Status
    ): Single<List<TransactionData>>

    @Query("DELETE FROM `Transaction`")
    abstract fun delete(): Completable

    @Query("SELECT * FROM `Transaction` WHERE id == :id")
    abstract fun getById(id: String): Single<TransactionData>

}