package com.bitmark.registry.data.model

import androidx.room.*
import com.bitmark.apiservice.utils.record.BitmarkRecord
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Entity(
    tableName = "Bitmark",
    indices = [(Index(
        value = ["id"],
        unique = true
    )), (Index(value = ["confirmed_at"])), (Index(
        value = ["asset_id"]
    )), (Index(value = ["block_number"]))]
)
data class BitmarkData(
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
    val issuer: String,

    @Expose
    val offset: Long,

    @Expose
    val owner: String,

    @Expose
    val status: Status,

    @Expose
    var seen: Boolean = false

) {

    // ignore value declare following
    @Ignore
    var asset: AssetData? = null

    enum class Status(val value: String) {
        ISSUING("issuing"), TRANSFERRING("transferring"), OFFERING("offering"), SETTLED(
            "settled"
        ),
        TO_BE_DELETED("to_be_deleted"), TO_BE_TRANSFERRED("to_be_transferred");

        companion object {
            fun from(value: String): Status? = when (value) {
                "issuing" -> ISSUING
                "transferring" -> TRANSFERRING
                "offering" -> OFFERING
                "settled" -> SETTLED
                "to_be_deleted" -> TO_BE_DELETED
                "to_be_transferred" -> TO_BE_TRANSFERRED
                else -> null
            }
        }
    }

    companion object {
        fun map(status: BitmarkRecord.Status): Status = when (status) {
            BitmarkRecord.Status.ISSUING -> Status.ISSUING
            BitmarkRecord.Status.OFFERING -> Status.OFFERING
            BitmarkRecord.Status.SETTLED -> Status.SETTLED
            BitmarkRecord.Status.TRANSFERRING -> Status.TRANSFERRING
        }
    }
}