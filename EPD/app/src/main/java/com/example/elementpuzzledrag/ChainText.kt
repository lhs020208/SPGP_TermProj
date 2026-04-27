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

class ChainText private constructor() : IGameObject, IRecyclable {
    companion object {
        private const val TEXT_WIDTH = 200f
        private const val TEXT_HEIGHT = 70f
        private const val JUMP_HEIGHT = 100f
        private const val DURATION = 0.5f

        fun get(
            gctx: GameContext,
            world: World<Layer>,
            centerX: Float,
            centerY: Float,
            chainNumber: Int,
        ): ChainText {
            val chainText = world.obtain(ChainText::class.java) ?: ChainText()
            return chainText.init(
                world = world,
                centerX = centerX,
                centerY = centerY,
                chainNumber = chainNumber,
            )
        }
    }

    private lateinit var world: World<Layer>

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = TEXT_HEIGHT
        alpha = 0
    }

    private var text = ""
    private var startX = 0f
    private var startY = 0f
    private var x = 0f
    private var y = 0f
    private var elapsed = 0f
    private var active = false

    private fun init(
        world: World<Layer>,
        centerX: Float,
        centerY: Float,
        chainNumber: Int,
    ): ChainText {
        this.world = world

        text = "Chain $chainNumber"
        startX = centerX
        startY = centerY
        x = startX
        y = startY
        elapsed = 0f
        active = true

        paint.color = Color.WHITE
        paint.textSize = TEXT_HEIGHT
        paint.alpha = 0
        updateTextScaleX()

        return this
    }

    private fun updateTextScaleX() {
        paint.textScaleX = 1f

        val measuredWidth = paint.measureText(text)
        paint.textScaleX = if (measuredWidth > 0f) {
            TEXT_WIDTH / measuredWidth
        } else {
            1f
        }
    }

    override fun update(gctx: GameContext) {
        if (!active) return

        elapsed += gctx.frameTime
        val t = (elapsed / DURATION).coerceIn(0f, 1f)

        x = startX
        y = startY - 4f * JUMP_HEIGHT * t * (1f - t)

        paint.alpha = alphaAt(t)

        if (t >= 1f) {
            active = false
            world.remove(this, Layer.OVERLAY)
        }
    }

    private fun alphaAt(t: Float): Int {
        val alpha = when {
            t < 0.25f -> {
                255f * (t / 0.25f)
            }

            t < 0.75f -> {
                255f
            }

            else -> {
                255f * ((1f - t) / 0.25f)
            }
        }

        return alpha.roundToInt().coerceIn(0, 255)
    }

    override fun draw(canvas: Canvas) {
        if (!active) return

        val fontMetrics = paint.fontMetrics
        val baseline = y - (fontMetrics.ascent + fontMetrics.descent) / 2f

        canvas.drawText(text, x, baseline, paint)
    }

    override fun onRecycle() {
        active = false
        elapsed = 0f
        paint.alpha = 0
        paint.textScaleX = 1f
    }
}