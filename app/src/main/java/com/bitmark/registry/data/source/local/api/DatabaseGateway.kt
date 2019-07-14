package com.bitmark.registry.data.source.local.api

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.TransactionData
import com.bitmark.registry.data.source.local.api.converter.*
import com.bitmark.registry.data.source.local.api.dao.AssetDao
import com.bitmark.registry.data.source.local.api.dao.BitmarkDao
import com.bitmark.registry.data.source.local.api.dao.TransactionDao


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Database(
    entities = [TransactionData::class, AssetData::class, BitmarkData::class],
    version = 1
)
@TypeConverters(
    LinkedTreeMapConverter::class,
    AssetStatusConverter::class,
    BitmarkStatusConverter::class,
    TransactionStatusConverter::class,
    HeadConverter::class
)
abstract class DatabaseGateway : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = BuildConfig.APPLICATION_ID
    }

    abstract fun transactionDao(): TransactionDao

    abstract fun assetDao(): AssetDao

    abstract fun bitmarkDao(): BitmarkDao

}