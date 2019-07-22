package com.bitmark.registry.data.model

import androidx.room.*
import com.bitmark.apiservice.utils.record.TransactionRecord
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Entity(
    tableName = "Transaction",
    indices = [(Index(
        value = ["id"],
        unique = true
    )), Index(value = ["bitmark_id"]), (Index(
        value = ["asset_id"]
    )), (Index(value = ["block_number"]))]
)
data class TransactionData(
    @Expose
    @PrimaryKey
    val id: String,

    @Expose
    val owner: String,

    @Expose
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,

    @Expose
    val head: Head?,

    @Expose
    val status: Status,

    @Expose
    @ColumnInfo(name = "block_number")
    @SerializedName("block_number")
    val blockNumber: Long,

    @Expose
    @ColumnInfo(name = "block_offset")
    @SerializedName("block_offset")
    val blockOffset: Long,

    @Expose
    val offset: Long,

    @Expose
    @ColumnInfo(name = "expiresAt")
    @SerializedName("expiresAt")
    val expiresAt: String?,

    @Expose
    @ColumnInfo(name = "pay_id")
    @SerializedName("pay_id")
    val payId: String,

    @Expose
    @ColumnInfo(name = "previous_id")
    @SerializedName("previous_id")
    val previousId: String?,

    @Expose
    @ColumnInfo(name = "bitmark_id")
    @SerializedName("bitmark_id")
    val bitmarkId: String,

    @Expose
    @ColumnInfo(name = "countersign")
    @SerializedName("countersign")
    val counterSig: Boolean,

    @Expose
    @ColumnInfo(name = "previous_owner")
    @SerializedName("previous_owner")
    val previousOwner: String?,

    @Expose
    val confirmation: Int

) {

    @Ignore
    var asset: AssetData? = null

    @Ignore
    var block: BlockData? = null

    enum class Status(val value: String) {
        CONFIRMED("confirmed"), PENDING("pending");

        companion object {
            fun from(value: String): Status? = when (value) {
                "confirmed" -> CONFIRMED
                "pending" -> PENDING
                else -> null
            }
        }
    }

    companion object {
        fun map(status: TransactionRecord.Status): Status = when (status) {
            TransactionRecord.Status.PENDING -> Status.PENDING
            TransactionRecord.Status.CONFIRMED -> Status.CONFIRMED
        }
    }
}