package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Paint

class Drop(
    gctx: GameContext,
    var row: Int,
    var col: Int,
    var type: DropType,
) : Sprite(gctx, type.toResId()) {

    private val drawPaint = Paint()
    private var isHolding = false
    private var isAnimating = false
    private var animStartX = 0f
    private var animStartY = 0f
    private var animTargetX = 0f
    private var animTargetY = 0f
    private var animElapsed = 0f
    private var animDuration = 0f

    companion object {
        const val CELL_SIZE = 150f
        const val DROP_SIZE = 120f

        const val FIRST_CENTER_X = 75f

        const val BOARD_TOP = 850f
        const val BOTTOM_VISIBLE_CENTER_Y = 1525f

        const val HOLD_SIZE = 150f
    }

    init {
        width = DROP_SIZE
        height = DROP_SIZE
        updatePosition()
    }

    fun updatePosition() {
        x = FIRST_CENTER_X + CELL_SIZE * col
        y = BOTTOM_VISIBLE_CENTER_Y - CELL_SIZE * row
    }

    fun setGridPosition(row: Int, col: Int) {
        this.row = row
        this.col = col

        val (centerX, centerY) = getGridCenter(row, col)
        setCenter(centerX, centerY)

        isAnimating = false
    }

    fun animateToGrid(row: Int, col: Int, duration: Float) {
        this.row = row
        this.col = col

        animStartX = x
        animStartY = y

        val (targetX, targetY) = getGridCenter(row, col)
        animTargetX = targetX
        animTargetY = targetY

        animElapsed = 0f
        animDuration = duration
        isAnimating = true
    }

    private fun getGridCenter(row: Int, col: Int): Pair<Float, Float> {
        val centerX = Board.BOARD_LEFT + CELL_SIZE * (col + 0.5f)
        val centerY = Board.VISIBLE_BOTTOM - CELL_SIZE * (row + 0.5f)
        return centerX to centerY
    }

    fun setHolding(holding: Boolean) {
        isHolding = holding

        if (holding) {
            width = HOLD_SIZE
            height = HOLD_SIZE
            drawPaint.alpha = 192
        } else {
            width = DROP_SIZE
            height = DROP_SIZE
            drawPaint.alpha = 255
        }
    }

    override fun draw(canvas: Canvas) {
        syncDstRect()
        canvas.drawBitmap(bitmap, srcRect, dstRect, drawPaint)
    }

    override fun update(gctx: GameContext) {
        if (!isAnimating) return

        animElapsed += gctx.frameTime
        val t = if (animDuration <= 0f) 1f else (animElapsed / animDuration).coerceIn(0f, 1f)

        val newX = animStartX + (animTargetX - animStartX) * t
        val newY = animStartY + (animTargetY - animStartY) * t
        setCenter(newX, newY)

        if (t >= 1f) {
            isAnimating = false
            setCenter(animTargetX, animTargetY)
        }
    }
}