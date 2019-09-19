package com.bitmark.registry.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bitmark.registry.data.model.AssetData
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-09-16
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Entity(
    tableName = "AssetR",
    indices = [(Index(
        value = ["id"],
        unique = true
    )), (Index(value = ["block_number"]))]
)
data class AssetDataR(
    @Expose
    @PrimaryKey
    val id: String,

    @Expose
    @SerializedName("block_number")
    @ColumnInfo(name = "block_number")
    val blockNumber: Long,

    @Expose
    @SerializedName("block_offset")
    @ColumnInfo(name = "block_offset")
    val blockOffset: Long,

    @Expose
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String?,

    @Expose
    @SerializedName("expires_at")
    @ColumnInfo(name = "expires_at")
    val expiresAt: String?,

    @Expose
    val fingerprint: String,

    @Expose
    val metadata: Map<String, String>?,

    @Expose
    val name: String?,

    @Expose
    val offset: Long,

    @Expose
    val registrant: String,

    @Expose
    val status: AssetData.Status
)