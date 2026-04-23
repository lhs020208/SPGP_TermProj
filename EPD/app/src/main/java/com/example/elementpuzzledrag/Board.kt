package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kr.ac.tukorea.ge.spgp2026.a2dg.util.Gauge
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
        private const val ORTHO_SWAP_THRESHOLD = 75f
        private const val DIAGONAL_SWAP_THRESHOLD = 55f
        private const val SWAP_ANIMATION_DURATION = 0.08f
        const val HOLD_LIMIT_SECONDS = 20f
        private const val GAUGE_THICKNESS = 0.04f
        private const val GAUGE_GAP = 14f
    }

    private val drops = Array(ROWS) { arrayOfNulls<Drop>(COLS) }
    private var holdingDrop: Drop? = null
    private var holdingRow = -1
    private var holdingCol = -1
    private val holdGauge = Gauge(
        thickness = GAUGE_THICKNESS,
        fgColor = Color.rgb(255, 96, 64),
        bgColor = Color.argb(160, 70, 70, 70),
    )

    private var timerStarted = false
    private var remainingHoldTime = HOLD_LIMIT_SECONDS

    init {
        fillInitialDrops()
    }

    private fun getCellCenter(row: Int, col: Int): Pair<Float, Float> {
        val centerX = BOARD_LEFT + Drop.CELL_SIZE * (col + 0.5f)
        val centerY = VISIBLE_BOTTOM - Drop.CELL_SIZE * (row + 0.5f)
        return centerX to centerY
    }

    private fun findSwapCandidateFromOffset(): Pair<Int, Int>? {
        val held = holdingDrop ?: return null

        val (centerX, centerY) = getCellCenter(holdingRow, holdingCol)
        val dx = held.x - centerX
        val dy = held.y - centerY

        val colStepForDiagonal = when {
            dx >= DIAGONAL_SWAP_THRESHOLD -> 1
            dx <= -DIAGONAL_SWAP_THRESHOLD -> -1
            else -> 0
        }

        val rowStepForDiagonal = when {
            dy <= -DIAGONAL_SWAP_THRESHOLD -> 1
            dy >= DIAGONAL_SWAP_THRESHOLD -> -1
            else -> 0
        }

        if (rowStepForDiagonal != 0 && colStepForDiagonal != 0) {
            val targetRow = holdingRow + rowStepForDiagonal
            val targetCol = holdingCol + colStepForDiagonal

            if (targetRow in 0 until VISIBLE_ROWS && targetCol in 0 until COLS) {
                return targetRow to targetCol
            }
        }

        val absDx = kotlin.math.abs(dx)
        val absDy = kotlin.math.abs(dy)

        if (absDx >= ORTHO_SWAP_THRESHOLD || absDy >= ORTHO_SWAP_THRESHOLD) {
            if (absDx >= absDy) {
                val targetCol = holdingCol + if (dx > 0f) 1 else -1
                if (targetCol in 0 until COLS) {
                    return holdingRow to targetCol
                }
            } else {
                val targetRow = holdingRow + if (dy < 0f) 1 else -1
                if (targetRow in 0 until VISIBLE_ROWS) {
                    return targetRow to holdingCol
                }
            }
        }

        return null
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
        timerStarted = false
        remainingHoldTime = HOLD_LIMIT_SECONDS
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

        if (rowDiff > 1 || colDiff > 1) return
        if (rowDiff == 0 && colDiff == 0) return

        val other = drops[targetRow][targetCol] ?: return

        drops[holdingRow][holdingCol] = other
        drops[targetRow][targetCol] = held

        other.animateToGrid(holdingRow, holdingCol, SWAP_ANIMATION_DURATION)

        held.row = targetRow
        held.col = targetCol
        holdingRow = targetRow
        holdingCol = targetCol

        if (!timerStarted) {
            timerStarted = true
            remainingHoldTime = HOLD_LIMIT_SECONDS
        }
    }

    override fun update(gctx: GameContext) {
        if (!timerStarted || holdingDrop == null) return

        remainingHoldTime -= gctx.frameTime
        if (remainingHoldTime <= 0f) {
            remainingHoldTime = 0f
            clearHold()
        }
    }

    override fun draw(canvas: Canvas) {
        if (!timerStarted) return

        val drop = holdingDrop ?: return
        val progress = (remainingHoldTime / HOLD_LIMIT_SECONDS).coerceIn(0f, 1f)

        val gaugeWidth = drop.width
        val gaugeX = drop.x - gaugeWidth / 2f
        val gaugeY = drop.y - drop.height / 2f - GAUGE_GAP

        holdGauge.draw(canvas, gaugeX, gaugeY, gaugeWidth, progress)
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

                val candidate = findSwapCandidateFromOffset()
                if (candidate != null) {
                    trySwapHoldingWith(candidate.first, candidate.second)
                }
                return true
            }
        }
        return false
    }
}