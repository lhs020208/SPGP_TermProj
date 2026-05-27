package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.roundToInt

class GameClearScene(
    gctx: GameContext,
) : Scene(gctx) {

    companion object {
        private const val INITIAL_BLACK_SECONDS = 0.15f
        private const val YOU_WIN_FADE_IN_SECONDS = 0.35f
        private const val RESTART_FADE_IN_SECONDS = 0.35f

        private const val YOU_WIN_TEXT_SIZE = 96f
        private const val RESTART_TEXT_SIZE = 44f
        private const val RESTART_TEXT_GAP = 90f
    }

    private var elapsed = 0f
    private var youWinAlpha = 0
    private var restartAlpha = 0

    private val bgPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val youWinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = YOU_WIN_TEXT_SIZE
        alpha = 0
    }

    private val restartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = RESTART_TEXT_SIZE
        alpha = 0
    }

    override fun update(gctx: GameContext) {
        elapsed += gctx.frameTime

        val youWinStart = INITIAL_BLACK_SECONDS
        val youWinEnd = youWinStart + YOU_WIN_FADE_IN_SECONDS

        val restartStart = youWinEnd
        val restartEnd = restartStart + RESTART_FADE_IN_SECONDS

        youWinAlpha = when {
            elapsed < youWinStart -> 0
            elapsed >= youWinEnd -> 255
            else -> {
                val t = (elapsed - youWinStart) / YOU_WIN_FADE_IN_SECONDS
                (255f * t).roundToInt().coerceIn(0, 255)
            }
        }

        restartAlpha = when {
            elapsed < restartStart -> 0
            elapsed >= restartEnd -> 255
            else -> {
                val t = (elapsed - restartStart) / RESTART_FADE_IN_SECONDS
                (255f * t).roundToInt().coerceIn(0, 255)
            }
        }

        youWinPaint.alpha = youWinAlpha
        restartPaint.alpha = restartAlpha
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(
            0f,
            0f,
            gctx.metrics.width,
            gctx.metrics.height,
            bgPaint,
        )

        val centerX = gctx.metrics.width / 2f
        val centerY = gctx.metrics.height / 2f

        drawCenteredText(
            canvas = canvas,
            text = "You Win",
            centerX = centerX,
            centerY = centerY,
            paint = youWinPaint,
        )

        drawCenteredText(
            canvas = canvas,
            text = "Tap to restart",
            centerX = centerX,
            centerY = centerY + RESTART_TEXT_GAP,
            paint = restartPaint,
        )
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint,
    ) {
        if (paint.alpha <= 0) return

        val baseline = centerY -
                (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f

        canvas.drawText(text, centerX, baseline, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) {
            return true
        }

        if (restartAlpha < 255) {
            return true
        }

        gctx.sceneStack.popAll()
        return true
    }
}