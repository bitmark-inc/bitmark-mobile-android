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
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

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