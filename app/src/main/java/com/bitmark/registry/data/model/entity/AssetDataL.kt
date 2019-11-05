/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bitmark.registry.data.model.AssetData
import com.google.gson.annotations.Expose

@Entity(
    tableName = "AssetL",
    indices = [(Index(
        value = ["id"],
        unique = true
    )), (Index(
        value = ["asset_id"],
        unique = true
    ))]
)
data class AssetDataL(
    @Expose
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,

    @Expose
    @ColumnInfo(name = "asset_id")
    val assetId: String,

    @Expose
    @ColumnInfo(name = "asset_type")
    val type: AssetData.Type = AssetData.Type.UNKNOWN
)