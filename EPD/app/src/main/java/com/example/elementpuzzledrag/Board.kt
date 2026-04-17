package com.example.elementpuzzledrag

import android.graphics.Canvas
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.random.Random

class Board(
    private val gctx: GameContext,
    private val world: World<Layer>,
) : IGameObject {

    companion object {
        const val COLS = 6
        const val ROWS = 10
        const val VISIBLE_ROWS = 5

        const val BOARD_LEFT = 0f
        const val BOARD_TOP = 850f
        const val BOARD_RIGHT = BOARD_LEFT + COLS * Drop.CELL_SIZE
        const val VISIBLE_BOTTOM = BOARD_TOP + VISIBLE_ROWS * Drop.CELL_SIZE
    }

    private val drops = Array(ROWS) { arrayOfNulls<Drop>(COLS) }
    private var holdingDrop: Drop? = null
    private var holdingRow = -1
    private var holdingCol = -1

    init {
        fillInitialDrops()
    }

    private fun fillInitialDrops() {
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                val drop = Drop(
                    gctx = gctx,
                    row = row,
                    col = col,
                    type = randomDropType(),
                )
                drops[row][col] = drop
                world.add(drop, Layer.BOARD)
            }
        }
    }

    private fun randomDropType(): DropType {
        val values = DropType.entries
        return values[Random.nextInt(values.size)]
    }

    fun getDrop(row: Int, col: Int): Drop? {
        if (row !in 0 until ROWS) return null
        if (col !in 0 until COLS) return null
        return drops[row][col]
    }

    private fun findVisibleCell(screenX: Float, screenY: Float): Pair<Int, Int>? {
        val pt = gctx.metrics.fromScreen(screenX, screenY)

        if (pt.x < BOARD_LEFT || pt.x >= BOARD_RIGHT) return null
        if (pt.y < BOARD_TOP || pt.y >= VISIBLE_BOTTOM) return null

        val col = ((pt.x - BOARD_LEFT) / Drop.CELL_SIZE).toInt()
        val rowFromTop = ((pt.y - BOARD_TOP) / Drop.CELL_SIZE).toInt()
        val row = (VISIBLE_ROWS - 1) - rowFromTop

        return row to col
    }

    private fun findVisibleCellAtWorld(worldX: Float, worldY: Float): Pair<Int, Int>? {
        if (worldX < BOARD_LEFT || worldX >= BOARD_RIGHT) return null
        if (worldY < BOARD_TOP || worldY >= VISIBLE_BOTTOM) return null

        val col = ((worldX - BOARD_LEFT) / Drop.CELL_SIZE).toInt()
        val rowFromTop = ((worldY - BOARD_TOP) / Drop.CELL_SIZE).toInt()
        val row = (VISIBLE_ROWS - 1) - rowFromTop

        return row to col
    }

    private fun beginHold(row: Int, col: Int) {
        val drop = drops[row][col] ?: return

        clearHold()

        holdingDrop = drop
        holdingRow = row
        holdingCol = col

        world.remove(drop, Layer.BOARD)
        world.add(drop, Layer.HOLDING)

        drop.setHolding(true)
    }

    private fun clearHold() {
        holdingDrop?.let { drop ->
            if (holdingRow >= 0 && holdingCol >= 0) {
                drop.setGridPosition(holdingRow, holdingCol)
            }

            world.remove(drop, Layer.HOLDING)
            world.add(drop, Layer.BOARD)

            drop.setHolding(false)
        }
        holdingDrop = null
        holdingRow = -1
        holdingCol = -1
    }

    private fun moveHoldingDrop(screenX: Float, screenY: Float) {
        val drop = holdingDrop ?: return
        val pt = gctx.metrics.fromScreen(screenX, screenY)

        val minX = 0f
        val maxX = gctx.metrics.width
        val minY = BOARD_TOP
        val maxY = gctx.metrics.height

        drop.x = pt.x.coerceIn(minX, maxX)
        drop.y = pt.y.coerceIn(minY, maxY)
    }

    private fun trySwapHoldingWith(targetRow: Int, targetCol: Int) {
        val held = holdingDrop ?: return

        if (targetRow == holdingRow && targetCol == holdingCol) return

        val rowDiff = kotlin.math.abs(targetRow - holdingRow)
        val colDiff = kotlin.math.abs(targetCol - holdingCol)

        if (rowDiff + colDiff != 1) return

        val other = drops[targetRow][targetCol] ?: return

        drops[holdingRow][holdingCol] = other
        drops[targetRow][targetCol] = held

        other.setGridPosition(holdingRow, holdingCol)

        held.row = targetRow
        held.col = targetCol
        holdingRow = targetRow
        holdingCol = targetCol
    }

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
    }

    fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                val cell = findVisibleCell(event.x, event.y) ?: return false
                beginHold(cell.first, cell.second)
                moveHoldingDrop(event.x, event.y)
                return true
            }

            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                clearHold()
                return true
            }

            android.view.MotionEvent.ACTION_MOVE -> {
                val held = holdingDrop ?: return false

                moveHoldingDrop(event.x, event.y)

                val cell = findVisibleCellAtWorld(held.x, held.y)
                if (cell != null) {
                    trySwapHoldingWith(cell.first, cell.second)
                }
                return true
            }
        }
        return false
    }
}