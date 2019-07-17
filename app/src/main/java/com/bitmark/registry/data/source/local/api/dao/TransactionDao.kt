package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.bitmark.registry.data.model.TransactionData
import io.reactivex.Completable
import io.reactivex.Maybe


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
    ): Maybe<List<TransactionData>>

}