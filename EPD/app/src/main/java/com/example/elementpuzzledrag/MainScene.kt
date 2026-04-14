package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class MainScene(gctx: GameContext) : Scene(gctx) {
    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.LTGRAY
        strokeWidth = 2f
    }

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 4f
    }

    override fun draw(canvas: Canvas) {
        val width = gctx.metrics.width
        val height = gctx.metrics.height

        canvas.drawRect(0f, 0f, width, height, backgroundPaint)

        val cols = 9
        val rows = 16
        val cellWidth = width / cols
        val cellHeight = height / rows

        for (c in 0..cols) {
            val x = c * cellWidth
            canvas.drawLine(x, 0f, x, height, gridPaint)
        }

        for (r in 0..rows) {
            val y = r * cellHeight
            canvas.drawLine(0f, y, width, y, gridPaint)
        }

        canvas.drawRect(0f, 0f, width, height, borderPaint)
    }
}