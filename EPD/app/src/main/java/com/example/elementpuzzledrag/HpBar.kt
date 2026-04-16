package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class HpBar(
    gctx: GameContext,
    private val left: Float,
    private val top: Float,
    private val barWidth: Float,
    private val barHeight: Float,
    hpRatio: Float = 1f,
) : IGameObject {

    private val hp0Bitmap = gctx.res.getBitmap(R.mipmap.i_hp0)
    private val hp1Bitmap = gctx.res.getBitmap(R.mipmap.i_hp1)

    var hpRatio: Float = hpRatio
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
        val dst = RectF(left, top, left + barWidth, top + barHeight)
        canvas.drawBitmap(hp0Bitmap, null, dst, null)

        val visibleRatio = hpRatio.coerceIn(0f, 1f)
        if (visibleRatio <= 0f) return

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
}