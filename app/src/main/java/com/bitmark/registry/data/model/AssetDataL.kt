package com.bitmark.registry.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose


/**
 * @author Hieu Pham
 * @since 2019-09-16
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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
    val type: AssetData.Type
)