package com.prayerlink.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.prayerlink.app.data.datasource.prayerLinkDataStore
import androidx.room.Room
import com.prayerlink.app.data.local.PrayerCompletionDao
import com.prayerlink.app.data.local.PrayerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides application-scoped dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Provides the single DataStore instance to the Hilt graph. */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.prayerLinkDataStore

    @Provides
    @Singleton
    fun providePrayerDatabase(@ApplicationContext context: Context): PrayerDatabase {
        return Room.databaseBuilder(
            context,
            PrayerDatabase::class.java,
            "prayer_database"
        ).build()
    }

    @Provides
    @Singleton
    fun providePrayerCompletionDao(database: PrayerDatabase): PrayerCompletionDao {
        return database.prayerCompletionDao()
    }
}
