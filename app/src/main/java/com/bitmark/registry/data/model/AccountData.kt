package com.bitmark.registry.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * @author Hieu Pham
 * @since 2019-07-18
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Entity(
    tableName = "Account",
    indices = [Index(value = ["account_number"], unique = true)]
)
data class AccountData(
    @Expose
    @SerializedName("account_number")
    @ColumnInfo(name = "account_number")
    @PrimaryKey
    val accountNumber: String,

    @Expose
    @SerializedName("enc_pub_key")
    @ColumnInfo(name = "enc_pub_key")
    val encPubKey: String
) {
}