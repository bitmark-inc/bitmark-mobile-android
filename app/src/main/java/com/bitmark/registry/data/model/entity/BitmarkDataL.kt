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

@Entity(
    tableName = "BitmarkL",
    indices = [(Index(
        value = ["id"],
        unique = true
    )), Index(
        value = ["bitmark_id"],
        unique = true
    )]
)
data class BitmarkDataL(
    @Expose
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,

    @Expose
    @ColumnInfo(name = "bitmark_id")
    val bitmarkId: String,

    @Expose
    var seen: Boolean = false
)