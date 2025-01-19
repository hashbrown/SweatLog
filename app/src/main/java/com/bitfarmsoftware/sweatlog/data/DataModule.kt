package com.bitfarmsoftware.sweatlog.data

import android.content.Context
import androidx.room.Room
import com.bitfarmsoftware.sweatlog.data.db.SweatLogDao
import com.bitfarmsoftware.sweatlog.data.db.SweatLogDatabase
import com.bitfarmsoftware.sweatlog.service.BleDeviceService
import com.bitfarmsoftware.sweatlog.service.BLEDeviceServiceImpl
import com.bitfarmsoftware.sweatlog.service.BleDeviceServiceProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindsUserDataRepository(
        userDataRepository: LocalUserDataRepository,
    ): UserDataRepository

    @Binds
    internal abstract fun bindsDevicesRepository(
        devicesRepository: CombinedDevicesRepository,
    ): DevicesRepository

    @Binds
    internal abstract fun bindsLogsRepository(
        logsRepository: LogsRepositoryImpl,
    ): LogsRepository

}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideSweatLogDatabase(@ApplicationContext app: Context): SweatLogDatabase {
        return Room.databaseBuilder(
            app,
            SweatLogDatabase::class.java,
            "sweatlog_dd"
        ).build()
    }

    @Provides
    fun provideSweatLogDao(db: SweatLogDatabase): SweatLogDao = db.sweatLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideBleDeviceService(@ApplicationContext context: Context): BleDeviceServiceProvider {
        return BleDeviceServiceProvider(context)
    }

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}
