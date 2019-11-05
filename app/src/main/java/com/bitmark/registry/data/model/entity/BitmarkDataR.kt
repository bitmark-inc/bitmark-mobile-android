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
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.Head
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "BitmarkR",
    indices = [(Index(
        value = ["id"],
        unique = true
    )), (Index(value = ["confirmed_at"])), (Index(
        value = ["asset_id"]
    )), (Index(value = ["block_number"]))]
)
data class BitmarkDataR(
    @Expose
    @PrimaryKey
    val id: String,

    @Expose
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,

    @Expose
    @ColumnInfo(name = "block_number")
    @SerializedName("block_number")
    val blockNumber: Long,

    @Expose
    @ColumnInfo(name = "confirmed_at")
    @SerializedName("confirmed_at")
    val confirmedAt: String?,

    @Expose
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String?,

    @Expose
    val head: Head?,

    @Expose
    @ColumnInfo(name = "head_id")
    @SerializedName("head_id")
    val headId: String,

    @Expose
    @ColumnInfo(name = "issued_at")
    @SerializedName("issued_at")
    val issuedAt: String?,

    @Expose
    var issuer: String,

    @Expose
    val offset: Long,

    @Expose
    val owner: String,

    @Expose
    val status: BitmarkData.Status,

    @Expose
    val edition: Int? = null,

    // We are no longer support for music claiming so it would be removed later
    @Expose
    @ColumnInfo(name = "total_edition")
    var totalEdition: Int? = null,

    @Expose
    @ColumnInfo(name = "readable_issuer")
    var readableIssuer: String? = null
)