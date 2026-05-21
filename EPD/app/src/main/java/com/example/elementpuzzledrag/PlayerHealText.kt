package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IRecyclable
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.roundToInt

class PlayerHealText private constructor(
    private val gameContext: GameContext,
) : IGameObject, IRecyclable {

    companion object {
        private const val TEXT_WIDTH = 150f
        private const val TEXT_HEIGHT = 50f
        private const val DURATION = 0.5f
        private const val MOVE_DISTANCE = 50f
        private const val SCREEN_MARGIN = 8f

        fun get(
            gctx: GameContext,
            world: World<Layer>,
            healAmount: Int,
            centerX: Float,
            centerY: Float,
        ): PlayerHealText {
            val text = world.obtain(PlayerHealText::class.java)
                ?: PlayerHealText(gctx)

            return text.init(
                world = world,
                healAmount = healAmount,
                centerX = centerX,
                centerY = centerY,
            )
        }
    }

    private lateinit var world: World<Layer>

    private var text = ""
    private var startX = 0f
    private var startY = 0f
    private var x = 0f
    private var y = 0f
    private var elapsed = 0f
    private var finished = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0x00, 0xFF, 0x00)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = TEXT_HEIGHT
        alpha = 255
    }

    private fun init(
        world: World<Layer>,
        healAmount: Int,
        centerX: Float,
        centerY: Float,
    ): PlayerHealText {
        this.world = world

        text = "+$healAmount"

        val minX = SCREEN_MARGIN + TEXT_WIDTH / 2f
        val maxX = gameContext.metrics.width - SCREEN_MARGIN - TEXT_WIDTH / 2f

        startX = if (minX <= maxX) {
            centerX.coerceIn(minX, maxX)
        } else {
            gameContext.metrics.width / 2f
        }

        startY = centerY
        x = startX
        y = startY

        elapsed = 0f
        finished = false

        paint.color = Color.rgb(0x00, 0xFF, 0x00)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = TEXT_HEIGHT
        paint.alpha = 255

        fitTextWidthIfNeeded()

        return this
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

    override fun onRecycle() {
        text = ""
        startX = 0f
        startY = 0f
        x = 0f
        y = 0f
        elapsed = 0f
        finished = false

        paint.alpha = 255
        paint.textScaleX = 1f
    }
}