package com.overplay.blackmagicshinedemo.di

import android.content.Context
import com.overplay.blackmagicshinedemo.presentation.videoplayer.MediaPlayer
import com.overplay.blackmagicshinedemo.presentation.videoplayer.VideoPlayerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MediaPlayerModule {

    @Provides
    @Singleton
    fun getVideoPlayer(@ApplicationContext context: Context): MediaPlayer = VideoPlayerImpl(context)
}