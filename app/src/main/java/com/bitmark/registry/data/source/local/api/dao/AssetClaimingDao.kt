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
import com.bitmark.registry.data.model.entity.AssetClaimingData
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class AssetClaimingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(assetClaimings: List<AssetClaimingData>): Completable

    @Query("DELETE FROM AssetClaiming")
    abstract fun delete(): Completable

    @Query("SELECT * FROM AssetClaiming WHERE asset_id = :assetId AND created_at BETWEEN :from AND :to ORDER BY created_at DESC")
    abstract fun listByAssetIdCreatedAtRange(
        assetId: String,
        from: String,
        to: String
    ): Single<List<AssetClaimingData>>
}