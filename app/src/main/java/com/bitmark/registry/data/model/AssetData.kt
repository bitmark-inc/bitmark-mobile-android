package com.bitmark.registry.data.model

import androidx.room.*
import com.bitmark.apiservice.utils.record.AssetRecord
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.File


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Entity(tableName = "Asset", indices = [(Index(value = ["id"]))])
data class AssetData(
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
    val status: Status
) {

    // ignore value declare following
    @Ignore
    var file: File? = null

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
        fun map(status: AssetRecord.Status): Status = when (status) {
            AssetRecord.Status.PENDING -> Status.PENDING
            AssetRecord.Status.CONFIRMED -> Status.CONFIRMED
        }
    }

}

