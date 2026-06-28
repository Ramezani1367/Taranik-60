package com.tranik.app.di

import com.tranik.app.data.repository.Id3TagRepository
import com.tranik.app.data.repository.LyricsRepository
import com.tranik.app.data.repository.SettingsRepository
import com.tranik.app.data.repository.TrackRepository
import com.tranik.app.data.source.MediaStoreDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMediaStoreDataSource(
        @ApplicationContext context: android.content.Context
    ): MediaStoreDataSource {
        return MediaStoreDataSource(context)
    }

    @Provides
    @Singleton
    fun provideTrackRepository(
        mediaStoreDataSource: MediaStoreDataSource
    ): TrackRepository {
        return TrackRepository(mediaStoreDataSource)
    }

    @Provides
    @Singleton
    fun provideId3TagRepository(): Id3TagRepository {
        return Id3TagRepository()
    }

    @Provides
    @Singleton
    fun provideLyricsRepository(): LyricsRepository {
        return LyricsRepository()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: android.content.Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }
}
