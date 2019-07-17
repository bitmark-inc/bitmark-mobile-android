package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.bitmark.registry.data.model.BlockData
import io.reactivex.Completable
import io.reactivex.Maybe


/**
 * @author Hieu Pham
 * @since 2019-07-16
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Dao
abstract class BlockDao {

    @Insert(onConflict = REPLACE)
    abstract fun save(blocks: List<BlockData>): Completable

    @Insert(onConflict = REPLACE)
    abstract fun save(block: BlockData): Completable

    @Query("SELECT * FROM Block WHERE number = :blockNumber")
    abstract fun getByNumber(blockNumber: Long): Maybe<BlockData>
}