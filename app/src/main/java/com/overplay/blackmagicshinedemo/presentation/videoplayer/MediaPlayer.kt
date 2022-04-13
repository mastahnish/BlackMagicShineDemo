package com.overplay.blackmagicshinedemo.presentation.videoplayer

import com.google.android.exoplayer2.ExoPlayer

interface MediaPlayer {
    fun getPlayer(): ExoPlayer?
    fun initializePlayer(): ExoPlayer?
    fun play()
    fun release()
    fun stop()
    fun pause()
    fun volumeUp()
    fun volumeDown()
    fun seekForward()
    fun seekBack()
    fun reset()
    fun isPlaying(): Boolean?
}