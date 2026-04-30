package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class AttackUpBuffText(
    private val color: Int,
    private val right: Float,
    private val top: Float,
) : IGameObject {

    companion object {
        private const val TEXT = "x1.5"
        private const val TEXT_SIZE = 42f
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT_BOLD
        textSize = TEXT_SIZE
        color = this@AttackUpBuffText.color
    }

    override fun update(gctx: GameContext) {
        // 고정 UI라서 갱신 없음
    }

    override fun draw(canvas: Canvas) {
        val baseline = top - paint.fontMetrics.ascent
        canvas.drawText(TEXT, right, baseline, paint)
    }
}