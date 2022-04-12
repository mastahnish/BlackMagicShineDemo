package com.overplay.blackmagicshinedemo.presentation

import android.annotation.SuppressLint
import android.os.Build.*
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.util.Util
import com.overplay.blackmagicshinedemo.databinding.ActivityMainBinding
import com.overplay.blackmagicshinedemo.presentation.countdown.CountDownAnimation
import dagger.hilt.android.AndroidEntryPoint

/**
 * This is the main activity that holds the MediaSession and shows the player.
 * As this is the video app I'll go with the single-activity architecture
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initUi()
    }

    /**
     * DISCLAIMER: Android API level 24 and higher supports multiple windows.
     * As the app can be visible, but not active in split window mode, I need to initialize the player in onStart.
     * Android API level 24 and lower requires to wait as long as possible until I grab resources,
     * so need wait until onResume before initializing the player.
     */
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= VERSION_CODES.N) {
            initPlayerUi()
            initCountDown()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if ((Util.SDK_INT < VERSION_CODES.N || viewModel.getMediaPlayer().getPlayer() == null)) {
            initPlayerUi()
            initCountDown()
        }
    }

    /**
     * DISCLAIMER: In Android 6.0 (API level 23) and earlier there is no guarantee of when onStop() is called;
     * it could get called 5 seconds after your activity disappears.
     * Therefore, in Android versions earlier than 7.0, your app should stop playback in onPause().
     * In Android 7.0 and beyond, the system calls onStop() as soon as the activity becomes not visible, so this is not a problem.
     */
    override fun onPause() {
        super.onPause()
        if (VERSION.SDK_INT < VERSION_CODES.N) viewModel.getMediaPlayer().release()
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= VERSION_CODES.N) viewModel.getMediaPlayer().release()
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        window.setDecorFitsSystemWindows(false)
    }


    private fun initUi() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        hideSystemBars()
        initCountDown()
    }

    private fun initPlayerUi() {
        binding.videoView.player = viewModel.getMediaPlayer().initializePlayer()
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun initCountDown() {
        CountDownAnimation(binding.countdown, 4).apply {
            setAnimation(
                ScaleAnimation(
                    1.0f, 0.0f, 1.0f,
                    0.0f, Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                )
            )
            setCountDownListener(object : CountDownAnimation.CountDownListener{
                override fun onCountDownEnd(animation: CountDownAnimation?) {
                    viewModel.getMediaPlayer().play()
                }
            })
        }.start()
    }
}