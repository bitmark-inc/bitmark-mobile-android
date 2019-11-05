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
import com.bitmark.registry.data.model.entity.BlockData
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
abstract class BlockDao {

    @Insert(onConflict = REPLACE)
    abstract fun save(blocks: List<BlockData>): Completable

    @Insert(onConflict = REPLACE)
    abstract fun save(block: BlockData): Completable

    @Query("SELECT * FROM Block WHERE number = :blockNumber")
    abstract fun getByNumber(blockNumber: Long): Maybe<BlockData>

    @Query("DELETE FROM Block")
    abstract fun delete(): Completable
}