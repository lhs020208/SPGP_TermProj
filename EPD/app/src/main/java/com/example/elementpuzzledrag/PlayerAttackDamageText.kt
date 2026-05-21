package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IRecyclable
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class PlayerAttackDamageText private constructor() : IGameObject, IRecyclable {

    companion object {
        private const val BASE_TEXT_WIDTH = 200f
        private const val BASE_TEXT_HEIGHT = 70f
        private const val DURATION = 0.5f
        private const val MOVE_DISTANCE = 50f

        fun get(
            world: World<Layer>,
            damage: Int,
            color: Int,
            centerX: Float,
            centerY: Float,
            sizeScale: Float,
        ): PlayerAttackDamageText {
            val text = world.obtain(PlayerAttackDamageText::class.java)
                ?: PlayerAttackDamageText()

            return text.init(
                world = world,
                damage = damage,
                color = color,
                centerX = centerX,
                centerY = centerY,
                sizeScale = sizeScale,
            )
        }
    }

    private lateinit var world: World<Layer>

    private var text = ""

    private var textWidth = BASE_TEXT_WIDTH
    private var textHeight = BASE_TEXT_HEIGHT

    private var startX = 0f
    private var startY = 0f
    private var x = 0f
    private var y = 0f

    private var moveX = 0f
    private var moveY = 0f

    private var elapsed = 0f
    private var finished = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = BASE_TEXT_HEIGHT
        alpha = 255
    }

    private fun init(
        world: World<Layer>,
        damage: Int,
        color: Int,
        centerX: Float,
        centerY: Float,
        sizeScale: Float,
    ): PlayerAttackDamageText {
        this.world = world

        text = "-$damage"

        textWidth = BASE_TEXT_WIDTH * sizeScale
        textHeight = BASE_TEXT_HEIGHT * sizeScale

        val angle = Random.nextFloat() * 2f * PI.toFloat()
        moveX = cos(angle) * MOVE_DISTANCE
        moveY = sin(angle) * MOVE_DISTANCE

        startX = centerX
        startY = centerY
        x = startX
        y = startY

        elapsed = 0f
        finished = false

        paint.color = color
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = textHeight
        paint.alpha = 255

        fitTextWidthIfNeeded()

        return this
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

    override fun onRecycle() {
        text = ""

        textWidth = BASE_TEXT_WIDTH
        textHeight = BASE_TEXT_HEIGHT

        startX = 0f
        startY = 0f
        x = 0f
        y = 0f

        moveX = 0f
        moveY = 0f

        elapsed = 0f
        finished = false

        paint.alpha = 255
        paint.textScaleX = 1f
    }
}