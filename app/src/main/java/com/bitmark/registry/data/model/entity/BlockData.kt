package com.bitmark.registry.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-07-16
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Entity(
    tableName = "Block",
    indices = [(Index(
        value = ["hash"],
        unique = true
    )), (Index(value = ["bitmark_id"])), (Index(
        value = ["number"],
        unique = true
    ))]
)
data class BlockData(
    @Expose
    val number: Long,

    @Expose
    @PrimaryKey
    val hash: String,

    @Expose
    @ColumnInfo(name = "bitmark_id")
    @SerializedName("bitmark_id")
    val bitmarkId: String,

    @Expose
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String
)