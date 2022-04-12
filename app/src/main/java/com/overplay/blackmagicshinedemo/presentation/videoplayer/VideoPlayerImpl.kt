package com.overplay.blackmagicshinedemo.presentation.videoplayer

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.overplay.blackmagicshinedemo.constants.Constants
import com.overplay.blackmagicshinedemo.constants.Constants.Companion.VIDEO_START

/***
 * This is a specific implementation of MediaPlayer. For the need of this app there's only
 * one implementation - a Video Player. But wanted to showcase in that way that
 * the implementation is VideoPlayer-library-agnostic.
 */
class VideoPlayerImpl(private val context: Context) : MediaPlayer {
    private var exoPlayer: ExoPlayer? = null

    override fun getPlayer(): ExoPlayer? {
        return exoPlayer
    }

    override fun initializePlayer(): ExoPlayer? {
        with(context) {
            exoPlayer = ExoPlayer.Builder(this).setRenderersFactory(DefaultRenderersFactory(this))
                .setLoadControl(DefaultLoadControl()).setTrackSelector(DefaultTrackSelector(this))
                .build()
        }
        return exoPlayer
    }

    override fun play() {
        exoPlayer?.run {
            setMediaItem(MediaItem.fromUri(Uri.parse(Constants.VIDEO_URL)))
            playWhenReady = true
            prepare()
        }
    }

    override fun release() {
        exoPlayer?.run {
            stop()
            release()
        }
        exoPlayer = null
    }

    override fun stop() {
        exoPlayer?.stop()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun volumeUp() {
        exoPlayer?.increaseDeviceVolume()
    }

    override fun volumeDown() {
        exoPlayer?.decreaseDeviceVolume()
    }

    override fun seekForward() {
        exoPlayer?.seekForward()
    }

    override fun seekBack() {
        exoPlayer?.seekBack()
    }

    override fun reset() {
        exoPlayer?.seekTo(VIDEO_START)
    }

}