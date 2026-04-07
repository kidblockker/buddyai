package com.buddy.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class State { IDLE, LISTENING, THINKING, SPEAKING }

    private var currentState = State.IDLE

    private val waveAnimator = ValueAnimator.ofFloat(0f, 2f * PI.toFloat()).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            amplitude = targetAmplitude
            invalidate()
        }
    }

    private var phase = 0f
    private var amplitude = 0f
    private var targetAmplitude = 0f

    // Colors
    private val idleColor = Color.parseColor("#1A3A5C")
    private val listeningColor = Color.parseColor("#00D4FF")
    private val thinkingColor = Color.parseColor("#7C3AED")
    private val speakingColor = Color.parseColor("#00FF88")

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }

    private val path = Path()
    private val numberOfWaves = 3

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f

        val color = when (currentState) {
            State.IDLE -> idleColor
            State.LISTENING -> listeningColor
            State.THINKING -> thinkingColor
            State.SPEAKING -> speakingColor
        }

        for (i in 0 until numberOfWaves) {
            val waveAmplitude = amplitude * (1f - i * 0.25f) * (h * 0.35f)
            val frequency = 1f + i * 0.5f
            val wavePhase = phase + i * (PI.toFloat() / numberOfWaves)
            val alpha = (1f - i * 0.3f)

            path.reset()
            var firstPoint = true

            for (x in 0..w.toInt() step 2) {
                val angle = (x / w) * 2f * PI.toFloat() * frequency + wavePhase
                val y = centerY + waveAmplitude * sin(angle.toDouble()).toFloat()

                if (firstPoint) {
                    path.moveTo(x.toFloat(), y)
                    firstPoint = false
                } else {
                    path.lineTo(x.toFloat(), y)
                }
            }

            // Glow layer
            glowPaint.color = color
            glowPaint.alpha = (80 * alpha).toInt()
            glowPaint.strokeWidth = 8f
            canvas.drawPath(path, glowPaint)

            // Main line
            paint.color = color
            paint.alpha = (255 * alpha).toInt()
            paint.strokeWidth = 3f - i * 0.5f
            canvas.drawPath(path, paint)
        }

        // Center dot / pulse
        val dotRadius = if (currentState == State.IDLE) 4f else 6f + amplitude * 4f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(dotRadius * 2, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(w / 2f, centerY, dotRadius, dotPaint)
    }

    fun setState(state: State) {
        currentState = state
        targetAmplitude = when (state) {
            State.IDLE -> 0.05f
            State.LISTENING -> 0.4f
            State.THINKING -> 0.25f
            State.SPEAKING -> 0.6f
        }

        if (!waveAnimator.isRunning) {
            waveAnimator.start()
        }
    }

    fun setAmplitude(amp: Float) {
        targetAmplitude = amp.coerceIn(0f, 1f)
    }

    override fun onDetachedFromWindow() {
        waveAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        waveAnimator.start()
    }
}
