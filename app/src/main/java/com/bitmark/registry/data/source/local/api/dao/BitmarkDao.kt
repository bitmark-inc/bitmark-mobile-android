package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitmark.registry.data.model.BitmarkData
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Dao
abstract class BitmarkDao {

    @Query("SELECT * FROM Bitmark WHERE `offset` <= :offset ORDER BY `offset` DESC LIMIT :limit")
    abstract fun listByOffsetLimitDesc(
        offset: Long,
        limit: Int
    ): Maybe<List<BitmarkData>>

    @Query("SELECT * FROM Bitmark WHERE status == :status ORDER BY `offset` DESC")
    abstract fun listByStatusDesc(status: BitmarkData.Status): Maybe<List<BitmarkData>>

    @Query("SELECT MAX(`offset`) FROM Bitmark")
    abstract fun maxOffset(): Single<Long>

    @Query("SELECT MIN(`offset`) FROM Bitmark")
    abstract fun minOffset(): Single<Long>

    @Query("SELECT COUNT(*) FROM Bitmark")
    abstract fun count(): Single<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(bitmarks: List<BitmarkData>): Completable

    @Query("UPDATE Bitmark SET status = :status WHERE id = :bitmarkId")
    abstract fun updateStatus(
        bitmarkId: String,
        status: BitmarkData.Status
    ): Completable

    @Query("SELECT * FROM Bitmark WHERE id = :bitmarkId")
    abstract fun getById(bitmarkId: String): Maybe<BitmarkData>

    @Query("DELETE FROM Bitmark WHERE id = :bitmarkId")
    abstract fun deleteById(bitmarkId: String): Completable

}