package com.bitmark.registry.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.model.Head
import com.bitmark.registry.data.model.TransactionData
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-09-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Entity(
    tableName = "TransactionR",
    indices = [(Index(
        value = ["id"],
        unique = true
    )), Index(value = ["bitmark_id"]), (Index(
        value = ["asset_id"]
    )), (Index(value = ["block_number"]))]
)
data class TransactionDataR(
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
    val status: TransactionData.Status,

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
    fun isDeleteTx() = owner == BuildConfig.ZERO_ADDRESS
}