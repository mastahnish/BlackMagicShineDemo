package com.overplay.blackmagicshinedemo.presentation

import androidx.lifecycle.ViewModel
import com.overplay.blackmagicshinedemo.presentation.videoplayer.MediaPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val videoPlayer : MediaPlayer) : ViewModel() {

    fun getMediaPlayer() = videoPlayer
}