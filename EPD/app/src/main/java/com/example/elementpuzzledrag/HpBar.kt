package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class HpBar(
    gctx: GameContext,
    private val left: Float,
    private val top: Float,
    private val barWidth: Float,
    private val barHeight: Float,
    maxHp: Int,
    currentHp: Int = maxHp,
) : IGameObject {

    companion object {
        private const val TEXT_SIZE_RATIO = 0.52f
        private const val TEXT_SHADOW_OFFSET = 2f
    }

    private val hp0Bitmap = gctx.res.getBitmap(R.mipmap.i_hp0)
    private val hp1Bitmap = gctx.res.getBitmap(R.mipmap.i_hp1)

    val maxHp: Int = maxHp.coerceAtLeast(1)

    var currentHp: Int = currentHp.coerceIn(0, this.maxHp)
        private set

    val hpRatio: Float
        get() = currentHp.toFloat() / maxHp.toFloat()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = barHeight * TEXT_SIZE_RATIO
    }

    private val textShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = barHeight * TEXT_SIZE_RATIO
    }

    fun takeDamage(amount: Int) {
        if (amount <= 0) return
        currentHp = (currentHp - amount).coerceAtLeast(0)
    }

    fun heal(amount: Int) {
        if (amount <= 0) return
        currentHp = (currentHp + amount).coerceAtMost(maxHp)
    }

    fun setHp(value: Int) {
        currentHp = value.coerceIn(0, maxHp)
    }

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
        val dst = RectF(
            left,
            top,
            left + barWidth,
            top + barHeight,
        )

        canvas.drawBitmap(hp0Bitmap, null, dst, null)

        val visibleRatio = hpRatio.coerceIn(0f, 1f)

        if (visibleRatio > 0f) {
            val src = Rect(
                0,
                0,
                (hp1Bitmap.width * visibleRatio).toInt(),
                hp1Bitmap.height,
            )

            val fgDst = RectF(
                left,
                top,
                left + barWidth * visibleRatio,
                top + barHeight,
            )

            canvas.drawBitmap(hp1Bitmap, src, fgDst, null)
        }

        drawHpText(canvas)
    }

    private fun drawHpText(canvas: Canvas) {
        val text = "$currentHp / $maxHp"

        val textRight = left + barWidth - 16f
        val centerY = top + barHeight / 2f

        val baseline = centerY -
                (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f

        textPaint.textAlign = Paint.Align.RIGHT
        textShadowPaint.textAlign = Paint.Align.RIGHT

        canvas.drawText(
            text,
            textRight + TEXT_SHADOW_OFFSET,
            baseline + TEXT_SHADOW_OFFSET,
            textShadowPaint,
        )

        canvas.drawText(
            text,
            textRight,
            baseline,
            textPaint,
        )
    }
}