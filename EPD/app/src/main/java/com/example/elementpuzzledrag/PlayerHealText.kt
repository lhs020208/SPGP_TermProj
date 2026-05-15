package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.roundToInt

class PlayerHealText(
    private val gctx: GameContext,
    private val world: World<Layer>,
    healAmount: Int,
    centerX: Float,
    centerY: Float,
) : IGameObject {

    companion object {
        private const val TEXT_WIDTH = 150f
        private const val TEXT_HEIGHT = 50f
        private const val DURATION = 0.5f
        private const val MOVE_DISTANCE = 50f
        private const val SCREEN_MARGIN = 8f
    }

    private val text = "+$healAmount"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0x00, 0xFF, 0x00)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = TEXT_HEIGHT
        alpha = 255
    }

    private val startX: Float
    private val startY = centerY
    private var x = centerX
    private var y = centerY
    private var elapsed = 0f
    private var finished = false

    init {
        val minX = SCREEN_MARGIN + TEXT_WIDTH / 2f
        val maxX = gctx.metrics.width - SCREEN_MARGIN - TEXT_WIDTH / 2f

        startX = if (minX <= maxX) {
            centerX.coerceIn(minX, maxX)
        } else {
            gctx.metrics.width / 2f
        }

        x = startX
        fitTextWidthIfNeeded()
    }

    private fun fitTextWidthIfNeeded() {
        paint.textScaleX = 1f

        val measuredWidth = paint.measureText(text)
        if (measuredWidth > TEXT_WIDTH && measuredWidth > 0f) {
            paint.textScaleX = TEXT_WIDTH / measuredWidth
        }
    }

    override fun update(gctx: GameContext) {
        if (finished) return

        elapsed += gctx.frameTime
        val t = (elapsed / DURATION).coerceIn(0f, 1f)

        x = startX
        y = startY - MOVE_DISTANCE * t

        paint.alpha = (255f * (1f - t))
            .roundToInt()
            .coerceIn(0, 255)

        if (t >= 1f) {
            finished = true
            world.remove(this, Layer.HUD)
        }
    }

    override fun draw(canvas: Canvas) {
        if (finished) return

        val baseline = y -
                (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f

        canvas.drawText(text, x, baseline, paint)
    }
}