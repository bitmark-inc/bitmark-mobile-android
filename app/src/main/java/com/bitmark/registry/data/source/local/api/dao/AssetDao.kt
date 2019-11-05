/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local.api.dao

import androidx.room.*
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.entity.AssetDataL
import com.bitmark.registry.data.model.entity.AssetDataR
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class AssetDao {

    @Transaction
    @Query("SELECT * FROM AssetR WHERE id = :id")
    abstract fun getById(id: String): Single<AssetData>

    @Transaction
    @Query("SELECT * FROM AssetL WHERE asset_id = :assetId")
    abstract fun getLByAssetId(assetId: String): Single<AssetDataL>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveR(asset: AssetDataR): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveL(asset: AssetDataL): Completable

    @Query("DELETE FROM AssetR WHERE id = :id")
    abstract fun deleteR(id: String): Completable

    @Query("DELETE FROM AssetL WHERE asset_id = :assetId")
    abstract fun deleteL(assetId: String): Completable

    @Query("DELETE FROM AssetR")
    abstract fun deleteR(): Completable

    @Query("DELETE FROM AssetL")
    abstract fun deleteL(): Completable

    @Query("UPDATE AssetL SET asset_type = :type WHERE asset_id = :assetId")
    abstract fun updateTypeByAssetId(
        assetId: String,
        type: AssetData.Type
    ): Completable

    @Query("SELECT asset_type from AssetL WHERE asset_id = :assetId")
    abstract fun getTypeByAssetId(assetId: String): Single<AssetData.Type>

}