package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class PlayerAttackDamageText(
    private val world: World<Layer>,
    damage: Int,
    color: Int,
    centerX: Float,
    centerY: Float,
    sizeScale: Float,
) : IGameObject {

    companion object {
        private const val BASE_TEXT_WIDTH = 200f
        private const val BASE_TEXT_HEIGHT = 70f
        private const val DURATION = 0.5f
        private const val MOVE_DISTANCE = 50f
    }

    private val text = "-$damage"

    private val textWidth = BASE_TEXT_WIDTH * sizeScale
    private val textHeight = BASE_TEXT_HEIGHT * sizeScale

    private val angle = Random.nextFloat() * 2f * PI.toFloat()
    private val moveX = cos(angle) * MOVE_DISTANCE
    private val moveY = sin(angle) * MOVE_DISTANCE

    private val startX = centerX
    private val startY = centerY

    private var x = centerX
    private var y = centerY
    private var elapsed = 0f
    private var finished = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = textHeight
        alpha = 255
    }

    init {
        fitTextWidthIfNeeded()
    }

    private fun fitTextWidthIfNeeded() {
        paint.textScaleX = 1f

        val measuredWidth = paint.measureText(text)
        if (measuredWidth > textWidth && measuredWidth > 0f) {
            paint.textScaleX = textWidth / measuredWidth
        }
    }

    override fun update(gctx: GameContext) {
        if (finished) return

        elapsed += gctx.frameTime

        val t = (elapsed / DURATION).coerceIn(0f, 1f)

        x = startX + moveX * t
        y = startY + moveY * t

        paint.alpha = (255f * (1f - t))
            .roundToInt()
            .coerceIn(0, 255)

        if (t >= 1f) {
            finished = true
            world.remove(this, Layer.ATTACK_TEXT)
        }
    }

    override fun draw(canvas: Canvas) {
        if (finished) return

        val baseline = y -
                (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f

        canvas.drawText(text, x, baseline, paint)
    }
}