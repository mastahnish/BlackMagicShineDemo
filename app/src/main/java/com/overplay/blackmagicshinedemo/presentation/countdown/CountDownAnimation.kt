/*
 * Copyright (C) 2014 Ivan Ridao Freitas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.overplay.blackmagicshinedemo.presentation.countdown

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

import android.widget.TextView

/**
 * This is an Open-Source Library:
 * https://github.com/IvanRF/CountDownAnimation/blob/master/src/com/ivanrf/countdownanimation/CountDownAnimation.java
 */
class CountDownAnimation {

    private var mTextView: TextView? = null
    private var mAnimation: Animation? = null
    private var mStartCount = 0
    private var mCurrentCount = 0
    private var mListener: CountDownListener? = null

    private val mHandler: Handler = Handler(Looper.getMainLooper())

    private val mCountDown = Runnable {
        if (mCurrentCount > 0) {
            mTextView!!.text = mCurrentCount.toString() + ""
            mTextView!!.startAnimation(mAnimation)
            mCurrentCount--
        } else {
            mTextView!!.visibility = View.GONE
            if (mListener != null) mListener!!.onCountDownEnd(this@CountDownAnimation)
        }
    }

    /**
     *
     *
     * Creates a count down animation in the <var>textView</var>, starting from
     * <var>startCount</var>.
     *
     *
     *
     * By default, the class defines a fade out animation, which uses
     * [AlphaAnimation] from 1 to 0.
     *
     *
     * @param textView
     * The view where the count down will be shown
     * @param startCount
     * The starting count number
     */
    constructor (textView: TextView?, startCount: Int) {
        mTextView = textView
        mStartCount = startCount
        mAnimation = AlphaAnimation(1.0f, 0.0f)
        mAnimation?.duration = 1000
    }

    /**
     * Starts the count down animation.
     */
    fun start() {
        mHandler.removeCallbacks(mCountDown)
        mTextView!!.text = mStartCount.toString() + ""
        mTextView!!.visibility = View.VISIBLE
        mCurrentCount = mStartCount
        mHandler.post(mCountDown)
        for (i in 1..mStartCount) {
            mHandler.postDelayed(mCountDown, (i * 1000).toLong())
        }
    }

    /**
     * Cancels the count down animation.
     */
    fun cancel() {
        mHandler.removeCallbacks(mCountDown)
        mTextView!!.text = ""
        mTextView!!.visibility = View.GONE
    }

    /**
     * Sets the animation used during the count down. If the duration of the
     * animation for each number is not set, one second will be defined.
     */
    fun setAnimation(animation: Animation?) {
        mAnimation = animation
        if (mAnimation?.duration === 0L) mAnimation?.duration = 1000
    }

    /**
     * Returns the animation used during the count down.
     */
    fun getAnimation(): Animation? {
        return mAnimation
    }

    /**
     * Sets a new starting count number for the count down animation.
     *
     * @param startCount
     * The starting count number
     */
    fun setStartCount(startCount: Int) {
        mStartCount = startCount
    }

    /**
     * Returns the starting count number for the count down animation.
     */
    fun getStartCount(): Int {
        return mStartCount
    }

    /**
     * Binds a listener to this count down animation. The count down listener is
     * notified of events such as the end of the animation.
     *
     * @param listener
     * The count down listener to be notified
     */
    fun setCountDownListener(listener: CountDownListener?) {
        mListener = listener
    }

    /**
     * A count down listener receives notifications from a count down animation.
     * Notifications indicate count down animation related events, such as the
     * end of the animation.
     */
    interface CountDownListener {
        /**
         * Notifies the end of the count down animation.
         *
         * @param animation
         * The count down animation which reached its end.
         */
        fun onCountDownEnd(animation: CountDownAnimation?)
    }
}