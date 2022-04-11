package com.overplay.blackmagicshinedemo.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.overplay.blackmagicshinedemo.databinding.ActivityMainBinding
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

    private val viewModel : MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    companion object {

    }
}