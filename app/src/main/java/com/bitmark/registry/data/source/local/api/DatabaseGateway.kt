/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local.api

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.model.entity.*
import com.bitmark.registry.data.source.local.api.converter.*
import com.bitmark.registry.data.source.local.api.dao.*

@Database(
    entities = [TransactionDataR::class, AssetDataR::class, AssetDataL::class, BitmarkDataR::class, BitmarkDataL::class, BlockData::class, AccountData::class, AssetClaimingData::class],
    version = 1
)
@TypeConverters(
    LinkedTreeMapConverter::class,
    AssetStatusConverter::class,
    BitmarkStatusConverter::class,
    TransactionStatusConverter::class,
    HeadConverter::class,
    AssetClaimingStatusConverter::class,
    AssetTypeConverter::class
)
abstract class DatabaseGateway : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = BuildConfig.APPLICATION_ID
    }

    abstract fun transactionDao(): TransactionDao

    abstract fun assetDao(): AssetDao

    abstract fun bitmarkDao(): BitmarkDao

    abstract fun blockDao(): BlockDao

    abstract fun accountDao(): AccountDao

    abstract fun assetClaimingDao(): AssetClaimingDao

}