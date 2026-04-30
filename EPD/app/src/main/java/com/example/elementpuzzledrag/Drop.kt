package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IRecyclable
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import android.graphics.Canvas
import android.graphics.Paint

class Drop private constructor(
    private val gameContext: GameContext,
) : Sprite(gameContext, DropType.FIRE.toResId()), IRecyclable {

    var row = 0
    var col = 0
    var type = DropType.FIRE

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
        const val HOLD_SIZE = 150f

        fun get(gctx: GameContext, world: World<Layer>, row: Int, col: Int, type: DropType): Drop{
            val drop = world.obtain(Drop::class.java) ?: Drop(gctx)
            return drop.init(row, col, type)
        }
    }

    init {
        setHolding(false)
        setGridPosition(0, 0)
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

    fun init(row: Int, col: Int, type: DropType): Drop {
        this.row = row
        this.col = col
        this.type = type

        bitmap = gameContext.res.getBitmap(type.toResId())

        isAnimating = false
        animStartX = 0f
        animStartY = 0f
        animTargetX = 0f
        animTargetY = 0f
        animElapsed = 0f
        animDuration = 0f

        setHolding(false)
        setGridPosition(row, col)
        return this
    }

    fun changeType(newType: DropType) {
        if (type == newType) return

        type = newType
        bitmap = gameContext.res.getBitmap(newType.toResId())
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

    override fun onRecycle() {
        isAnimating = false
        animElapsed = 0f
        animDuration = 0f
        setHolding(false)
    }

}