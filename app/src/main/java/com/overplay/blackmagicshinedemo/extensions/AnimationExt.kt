package com.overplay.blackmagicshinedemo.extensions

import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import com.overplay.blackmagicshinedemo.presentation.countdown.CountDownAnimation

fun CountDownAnimation.scaleAnimation() {
    this.setAnimation(
        ScaleAnimation(
            1.0f, 0.0f, 1.0f,
            0.0f, Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
    )
}

fun CountDownAnimation.countdownListener(onCompleted: () -> Unit){
    this.setCountDownListener(object : CountDownAnimation.CountDownListener {
        override fun onCountDownEnd(animation: CountDownAnimation?) {
           onCompleted()
        }
    })
}