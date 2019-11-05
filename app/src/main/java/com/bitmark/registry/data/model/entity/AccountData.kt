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