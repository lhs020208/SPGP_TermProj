package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class FullScreenFadeOverlay(
    private val gctx: GameContext,
    color: Int = Color.BLACK,
) : IGameObject {

    var alpha: Int = 0
        set(value) {
            field = value.coerceIn(0, 255)
            paint.alpha = field
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
        alpha = 0
    }

    override fun update(gctx: GameContext) {
        // MainScene에서 alpha를 직접 제어한다.
    }

    override fun draw(canvas: Canvas) {
        if (alpha <= 0) return

        canvas.drawRect(
            0f,
            0f,
            gctx.metrics.width,
            gctx.metrics.height,
            paint,
        )
    }
}