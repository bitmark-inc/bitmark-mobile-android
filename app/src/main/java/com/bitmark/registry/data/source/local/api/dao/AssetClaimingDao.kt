package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitmark.registry.data.model.AssetClaimingData
import io.reactivex.Completable
import io.reactivex.Single


/**
 * @author Hieu Pham
 * @since 2019-08-12
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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