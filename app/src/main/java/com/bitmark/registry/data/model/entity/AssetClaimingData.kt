/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.model.entity

import androidx.room.*
import com.bitmark.registry.data.model.AssetData
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "AssetClaiming",
    indices = [(Index(
        value = ["id"],
        unique = true
    )), Index(value = ["asset_id"])]
)
data class AssetClaimingData(
    @Expose
    @PrimaryKey
    @SerializedName("id")
    val id: String,

    @Expose
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,

    @Expose
    @SerializedName("from")
    val from: String,

    @Expose
    @SerializedName("status")
    val status: Status,

    @Expose
    @SerializedName("info")
    val info: Map<String, String>?,

    @Expose
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String
) {

    @Ignore
    var asset: AssetData? = null

    enum class Status(val value: String) {
        @Expose
        @SerializedName("accepted")
        ACCEPTED("accepted"),

        @Expose
        @SerializedName("pending")
        PENDING("pending"),

        @Expose
        @SerializedName("rejected")
        REJECTED("rejected");

        companion object {
            fun from(value: String): Status? = when (value) {
                "accepted" -> ACCEPTED
                "pending" -> PENDING
                "rejected" -> REJECTED
                else -> null
            }
        }
    }
}