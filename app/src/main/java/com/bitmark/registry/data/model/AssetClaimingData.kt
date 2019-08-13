package com.bitmark.registry.data.model

import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-08-12
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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
    val id: String,

    @Expose
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,

    @Expose
    val from: String,

    @Expose
    val status: Status,

    @Expose
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