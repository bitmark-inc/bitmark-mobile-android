package com.bitmark.registry.data.source.local.api.dao

import androidx.room.*
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.entity.BitmarkDataL
import com.bitmark.registry.data.model.entity.BitmarkDataR
import io.reactivex.Completable
import io.reactivex.Single


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Dao
abstract class BitmarkDao {

    @Transaction
    @Query("SELECT * FROM BitmarkR WHERE owner = :owner AND `offset` <= :offset ORDER BY `offset` DESC LIMIT :limit")
    abstract fun listByOwnerOffsetLimitDesc(
        owner: String,
        offset: Long,
        limit: Int
    ): Single<List<BitmarkData>>

    @Transaction
    @Query("SELECT * FROM BitmarkR WHERE owner = :owner AND status == :status ORDER BY `offset` DESC")
    abstract fun listByOwnerStatusDesc(
        owner: String,
        status: BitmarkData.Status
    ): Single<List<BitmarkData>>

    @Transaction
    @Query("SELECT * FROM BitmarkR WHERE owner = :owner AND status IN (:status) ORDER BY `offset` DESC")
    abstract fun listByOwnerStatusDesc(
        owner: String,
        status: List<BitmarkData.Status>
    ): Single<List<BitmarkData>>

    @Transaction
    @Query("SELECT id FROM BitmarkR WHERE owner != :owner")
    abstract fun listIdNotOwnBy(owner: String): Single<List<String>>

    @Query("SELECT MAX(`offset`) FROM BitmarkR")
    abstract fun maxOffset(): Single<Long>

    @Query("SELECT MIN(`offset`) FROM BitmarkR")
    abstract fun minOffset(): Single<Long>

    @Query("SELECT MIN(`offset`) FROM BitmarkR WHERE status IN (:status)")
    abstract fun minOffset(status: Array<BitmarkData.Status>): Single<Long>

    @Query("SELECT COUNT(*) FROM BitmarkR")
    abstract fun count(): Single<Long>

    @Query("SELECT COUNT(*) FROM BitmarkR WHERE owner = :owner AND status NOT IN ('to_be_deleted', 'to_be_transferred')")
    abstract fun countUsable(owner: String): Single<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveR(bitmark: BitmarkDataR): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveL(bitmark: BitmarkDataL): Completable

    @Query("UPDATE BitmarkR SET status = :status WHERE id = :bitmarkId")
    abstract fun updateStatus(
        bitmarkId: String,
        status: BitmarkData.Status
    ): Completable

    @Transaction
    @Query("SELECT * FROM BitmarkR WHERE id = :bitmarkId")
    abstract fun getById(bitmarkId: String): Single<BitmarkData>

    @Query("SELECT * FROM BitmarkL WHERE bitmark_id = :bitmarkId")
    abstract fun getLByBmId(bitmarkId: String): Single<BitmarkDataL>

    @Query("DELETE FROM BitmarkR WHERE id = :bitmarkId")
    abstract fun deleteRById(bitmarkId: String): Completable

    @Query("DELETE FROM BitmarkL WHERE bitmark_id = :bitmarkId")
    abstract fun deleteLByBmId(bitmarkId: String): Completable

    @Query("UPDATE BitmarkL SET seen = 1 WHERE bitmark_id = :bitmarkId")
    abstract fun markSeen(bitmarkId: String): Completable

    @Query("SELECT COUNT(*) FROM BitmarkR WHERE asset_id = :assetId")
    abstract fun countRefSameAsset(assetId: String): Single<Long>

    @Transaction
    @Query("SELECT * FROM BitmarkR WHERE asset_id = :assetId")
    abstract fun listRefSameAsset(assetId: String): Single<List<BitmarkData>>

    @Query("DELETE FROM BitmarkR")
    abstract fun deleteR(): Completable

    @Query("DELETE FROM BitmarkL")
    abstract fun deleteL(): Completable

    @Query("SELECT COUNT(*) FROM BitmarkR R INNER JOIN BitmarkL L ON R.id = L.bitmark_id WHERE R.owner = :owner AND L.seen = 0 ")
    abstract fun countUnseen(owner: String): Single<Int>

}