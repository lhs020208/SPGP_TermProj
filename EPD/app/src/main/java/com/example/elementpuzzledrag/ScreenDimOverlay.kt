package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class ScreenDimOverlay(
    private val gctx: GameContext,
) : IGameObject {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 0, 0, 0)
        style = Paint.Style.FILL
    }

    override fun update(gctx: GameContext) {
        // 고정 오버레이
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(
            0f,
            0f,
            this.gctx.metrics.width,
            this.gctx.metrics.height,
            paint,
        )
    }
}