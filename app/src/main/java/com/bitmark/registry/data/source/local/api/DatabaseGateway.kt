package com.bitmark.registry.data.source.local.api

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.model.*
import com.bitmark.registry.data.source.local.api.converter.*
import com.bitmark.registry.data.source.local.api.dao.*


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Database(
    entities = [TransactionData::class, AssetData::class, BitmarkData::class, BlockData::class, AccountData::class, AssetClaimingData::class],
    version = 1
)
@TypeConverters(
    LinkedTreeMapConverter::class,
    AssetStatusConverter::class,
    BitmarkStatusConverter::class,
    TransactionStatusConverter::class,
    HeadConverter::class,
    AssetClaimingStatusConverter::class
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