package com.bitmark.registry.data.source

import android.content.Context
import androidx.room.Room
import com.bitmark.registry.data.source.local.AccountLocalDataSource
import com.bitmark.registry.data.source.local.AppLocalDataSource
import com.bitmark.registry.data.source.local.BitmarkLocalDataSource
import com.bitmark.registry.data.source.local.api.DatabaseApi
import com.bitmark.registry.data.source.local.api.DatabaseGateway
import com.bitmark.registry.data.source.remote.AccountRemoteDataSource
import com.bitmark.registry.data.source.remote.AppRemoteDataSource
import com.bitmark.registry.data.source.remote.BitmarkRemoteDataSource
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class RepositoryModule {

    @Singleton
    @Provides
    fun provideAccountRepo(
        remoteDataSource: AccountRemoteDataSource,
        localDataSource: AccountLocalDataSource
    ): AccountRepository {
        return AccountRepository(localDataSource, remoteDataSource)
    }

    @Singleton
    @Provides
    fun provideBitmarkRepo(
        remoteDataSource: BitmarkRemoteDataSource,
        localDataSource: BitmarkLocalDataSource
    ): BitmarkRepository {
        return BitmarkRepository(localDataSource, remoteDataSource)
    }

    @Singleton
    @Provides
    fun provideAppRepo(
        remoteDataSource: AppRemoteDataSource,
        localDataSource: AppLocalDataSource
    ): AppRepository {
        return AppRepository(localDataSource, remoteDataSource)
    }

    @Singleton
    @Provides
    fun provideDatabaseGateway(context: Context): DatabaseGateway {
        return Room.databaseBuilder(
            context, DatabaseGateway::class.java,
            DatabaseGateway.DATABASE_NAME
        )
            .build()
    }

    @Singleton
    @Provides
    fun provideDatabaseApi(databaseGateway: DatabaseGateway): DatabaseApi {
        return DatabaseApi(databaseGateway)
    }
}