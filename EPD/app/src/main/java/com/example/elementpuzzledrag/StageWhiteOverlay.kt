package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class StageWhiteOverlay(
    private val left: Float,
    private val top: Float,
    private val width: Float,
    private val height: Float,
) : IGameObject {

    var alpha: Int = 0
        set(value) {
            field = value.coerceIn(0, 255)
            paint.alpha = field
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 0
    }

    override fun update(gctx: GameContext) {
        // MainScene에서 alpha를 직접 제어한다.
    }

    override fun draw(canvas: Canvas) {
        if (alpha <= 0) return

        canvas.drawRect(
            left,
            top,
            left + width,
            top + height,
            paint,
        )
    }
}