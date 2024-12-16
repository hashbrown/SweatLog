package com.bitfarmsoftware.sweatlog.data

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindsUserDataRepository(
        userDataRepository: LocalUserDataRepository,
    ): UserDataRepository

    @Binds
    internal abstract fun bindsDevicesRepository(
        devicesRepository: CombinedDevicesRepository,
    ): DevicesRepository

}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}

//@Module
//@InstallIn(ActivityComponent::class)
//object DataProviders {
//    @Provides
//    fun provideRemoteDevicesDataSource(@ActivityContext activityContext: Context): RemoteDevicesDataSource {
//        return RemoteDevicesDataSource(activityContext)
//    }
//}