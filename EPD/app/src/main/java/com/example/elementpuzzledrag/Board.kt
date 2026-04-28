package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.Color
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kr.ac.tukorea.ge.spgp2026.a2dg.util.Gauge
import kotlin.random.Random

data class PlayerAttackResult(
    val chainCount: Int,
    val removedDropCounts: Map<DropType, Int>,
)

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
        private const val GRAVITY_ANIMATION_DURATION = SWAP_ANIMATION_DURATION
        private const val NEXT_CASCADE_DELAY = 1f
        const val HOLD_LIMIT_SECONDS = 20f
        private const val GAUGE_THICKNESS = 0.05f
        private const val GAUGE_GAP = 14f
        private const val MATCH_GROUP_REMOVE_INTERVAL = 0.5f
        private const val BEFORE_GRAVITY_DELAY = 0.8f

        private const val USE_DEBUG_VISIBLE_BOARD = true
        private val DEBUG_VISIBLE_BOARD_TOP_TO_BOTTOM = listOf(
            "RRBRBB",
            "BBGBGG",
            "GGYGYY",
            "YYPYPP",
            "PPHPHH",
        )
    }

    private val drops = Array(ROWS) { arrayOfNulls<Drop>(COLS) }
    private var holdingDrop: Drop? = null
    private var holdingRow = -1
    private var holdingCol = -1
    private var swappedDuringHold = false
    private val holdGauge = Gauge(
        thickness = GAUGE_THICKNESS,
        fgColor = Color.rgb(255, 96, 64),
        bgColor = Color.argb(160, 70, 70, 70),
    )

    private var timerStarted = false
    private var remainingHoldTime = HOLD_LIMIT_SECONDS
    private var resolvingMatches = false
    private val pendingMatchGroups = mutableListOf<MatchGroup>()
    private var matchRemoveDelay = 3f

    private var waitingBeforeGravity = false
    private var beforeGravityDelayRemaining = 0f

    private var gravityAnimating = false
    private var gravityAnimationRemaining = 10f
    private var waitingNextCascade = false
    private var nextCascadeDelayRemaining = 3f

    private var chainCount = 0
    private val removedDropCounts = DropType.entries
        .associateWith { 0 }
        .toMutableMap()
    private var attackResultReady = false

    private data class MatchGroup(
        val type: DropType,
        val cells: MutableList<Pair<Int, Int>> = mutableListOf(),
    )

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
                val drop = Drop.get(
                    gctx = gctx,
                    world = world,
                    row = row,
                    col = col,
                    type = initialDropTypeAt(row, col),
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

    fun getChainCount(): Int {
        return chainCount
    }

    fun getRemovedDropCount(type: DropType): Int {
        return removedDropCounts[type] ?: 0
    }

    fun getRemovedDropCounts(): Map<DropType, Int> {
        return removedDropCounts.toMap()
    }

    fun isAttackResultReady(): Boolean {
        return attackResultReady
    }

    fun consumeAttackResult(): PlayerAttackResult? {
        if (!attackResultReady) return null

        attackResultReady = false

        return PlayerAttackResult(
            chainCount = chainCount,
            removedDropCounts = removedDropCounts.toMap(),
        )
    }

    private fun charToDropType(ch: Char): DropType {
        return when (ch) {
            'R' -> DropType.FIRE
            'B' -> DropType.WATER
            'G' -> DropType.LEAF
            'Y' -> DropType.LIGHT
            'P' -> DropType.DARK
            'H' -> DropType.HP
            else -> error("Unknown drop code: $ch")
        }
    }

    private fun initialDropTypeAt(row: Int, col: Int): DropType {
        if (USE_DEBUG_VISIBLE_BOARD && row in 0 until VISIBLE_ROWS) {
            val lineIndexFromTop = VISIBLE_ROWS - 1 - row
            val ch = DEBUG_VISIBLE_BOARD_TOP_TO_BOTTOM[lineIndexFromTop][col]
            return charToDropType(ch)
        }

        return randomDropType()
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
        resetResolveResult()

        holdingDrop = drop
        holdingRow = row
        holdingCol = col

        world.moveBetweenLayers(drop, Layer.BOARD, Layer.HOLDING)

        drop.setHolding(true)
        swappedDuringHold = false
    }

    private fun clearHold() {
        holdingDrop?.let { drop ->
            if (holdingRow >= 0 && holdingCol >= 0) {
                drop.setGridPosition(holdingRow, holdingCol)
            }

            world.moveBetweenLayers(drop, Layer.HOLDING, Layer.BOARD)

            drop.setHolding(false)
        }
        holdingDrop = null
        holdingRow = -1
        holdingCol = -1
        timerStarted = false
        remainingHoldTime = HOLD_LIMIT_SECONDS
    }

    private fun resetResolveResult() {
        chainCount = 0

        for (type in DropType.entries) {
            removedDropCounts[type] = 0
        }

        attackResultReady = false
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
        swappedDuringHold = true

        if (!timerStarted) {
            timerStarted = true
            remainingHoldTime = HOLD_LIMIT_SECONDS
        }
    }

    private fun getGroupTopRow(group: MatchGroup): Int {
        return group.cells.maxOf { it.first }
    }

    private fun getGroupLeftColAtTop(group: MatchGroup): Int {
        val topRow = getGroupTopRow(group)
        return group.cells
            .filter { it.first == topRow }
            .minOf { it.second }
    }

    private fun sortMatchGroups(groups: List<MatchGroup>): List<MatchGroup> {
        return groups.sortedWith(
            compareByDescending<MatchGroup> { getGroupTopRow(it) }
                .thenBy { getGroupLeftColAtTop(it) }
        )
    }

    private fun findMatchGroups(): List<MatchGroup> {
        val matched = Array(VISIBLE_ROWS) { BooleanArray(COLS) }

        // 가로 검사
        for (row in 0 until VISIBLE_ROWS) {
            var col = 0
            while (col < COLS) {
                val startDrop = drops[row][col]
                if (startDrop == null) {
                    col++
                    continue
                }

                val type = startDrop.type
                var end = col + 1
                while (end < COLS && drops[row][end]?.type == type) {
                    end++
                }

                if (end - col >= 3) {
                    for (c in col until end) {
                        matched[row][c] = true
                    }
                }
                col = end
            }
        }

        // 세로 검사
        for (col in 0 until COLS) {
            var row = 0
            while (row < VISIBLE_ROWS) {
                val startDrop = drops[row][col]
                if (startDrop == null) {
                    row++
                    continue
                }

                val type = startDrop.type
                var end = row + 1
                while (end < VISIBLE_ROWS && drops[end][col]?.type == type) {
                    end++
                }

                if (end - row >= 3) {
                    for (r in row until end) {
                        matched[r][col] = true
                    }
                }
                row = end
            }
        }

        // 마킹된 칸들을 같은 타입 + 상하좌우 연결 기준으로 그룹화
        val visited = Array(VISIBLE_ROWS) { BooleanArray(COLS) }
        val groups = mutableListOf<MatchGroup>()
        val directions = arrayOf(
            intArrayOf(1, 0),
            intArrayOf(-1, 0),
            intArrayOf(0, 1),
            intArrayOf(0, -1),
        )

        for (row in 0 until VISIBLE_ROWS) {
            for (col in 0 until COLS) {
                if (!matched[row][col] || visited[row][col]) continue

                val type = drops[row][col]?.type ?: continue
                val group = MatchGroup(type)
                val queue = ArrayDeque<Pair<Int, Int>>()

                visited[row][col] = true
                queue.addLast(row to col)

                while (queue.isNotEmpty()) {
                    val (cr, cc) = queue.removeFirst()
                    group.cells.add(cr to cc)

                    for ((dr, dc) in directions) {
                        val nr = cr + dr
                        val nc = cc + dc

                        if (nr !in 0 until VISIBLE_ROWS) continue
                        if (nc !in 0 until COLS) continue
                        if (visited[nr][nc]) continue
                        if (!matched[nr][nc]) continue
                        if (drops[nr][nc]?.type != type) continue

                        visited[nr][nc] = true
                        queue.addLast(nr to nc)
                    }
                }

                groups.add(group)
            }
        }

        return groups
    }

    private fun getMatchGroupCenter(group: MatchGroup): Pair<Float, Float> {
        var sumX = 0f
        var sumY = 0f

        for ((row, col) in group.cells) {
            val (centerX, centerY) = getCellCenter(row, col)
            sumX += centerX
            sumY += centerY
        }

        val count = group.cells.size.coerceAtLeast(1)
        return sumX / count to sumY / count
    }

    private fun showChainText(group: MatchGroup, chainNumber: Int) {
        val (centerX, centerY) = getMatchGroupCenter(group)

        val chainText = ChainText.get(
            gctx = gctx,
            world = world,
            centerX = centerX,
            centerY = centerY,
            chainNumber = chainNumber,
        )

        world.add(chainText, Layer.OVERLAY)
    }

    private fun recordRemovedMatchGroup(group: MatchGroup, removedCount: Int) {
        if (removedCount <= 0) return

        chainCount += 1
        removedDropCounts[group.type] = (removedDropCounts[group.type] ?: 0) + removedCount
        attackResultReady = false

        showChainText(group, chainCount)
    }

    private fun removeMatchGroup(group: MatchGroup) {
        var removedCount = 0

        for ((row, col) in group.cells) {
            val drop = drops[row][col] ?: continue
            world.remove(drop, Layer.BOARD)
            drops[row][col] = null
            removedCount += 1
        }

        recordRemovedMatchGroup(group, removedCount)
    }

    private fun applyGravityAnimated(): Boolean {
        var movedAny = false

        for (col in 0 until COLS) {
            var writeRow = 0

            for (readRow in 0 until ROWS) {
                val drop = drops[readRow][col] ?: continue

                if (readRow != writeRow) {
                    drops[writeRow][col] = drop
                    drops[readRow][col] = null
                    drop.animateToGrid(writeRow, col, GRAVITY_ANIMATION_DURATION)
                    movedAny = true
                }

                writeRow++
            }

            for (row in writeRow until ROWS) {
                drops[row][col] = null
            }
        }

        return movedAny
    }

    private fun startBeforeGravityDelay() {
        waitingBeforeGravity = true
        beforeGravityDelayRemaining = BEFORE_GRAVITY_DELAY
    }

    private fun applyGravityOrFinish() {
        val moved = applyGravityAnimated()

        if (moved) {
            gravityAnimating = true
            gravityAnimationRemaining = GRAVITY_ANIMATION_DURATION
        } else {
            finishAfterGravityAnimation()
        }
    }

    private fun fillEmptyCellsRandomly() {
        for (col in 0 until COLS) {
            for (row in 0 until ROWS) {
                if (drops[row][col] != null) continue

                val drop = Drop.get(
                    gctx = gctx,
                    world = world,
                    row = row,
                    col = col,
                    type = randomDropType(),
                )
                drops[row][col] = drop
                world.add(drop, Layer.BOARD)
            }
        }
    }

    private fun finishAfterGravityAnimation() {
        waitingBeforeGravity = false
        beforeGravityDelayRemaining = 0f

        fillEmptyCellsRandomly()

        if (preparePendingMatchGroups()) {
            waitingNextCascade = true
            nextCascadeDelayRemaining = NEXT_CASCADE_DELAY
        } else {
            resolvingMatches = false
            matchRemoveDelay = 0f
            waitingNextCascade = false
            nextCascadeDelayRemaining = 0f
            attackResultReady = chainCount > 0
        }
    }

    private fun preparePendingMatchGroups(): Boolean {
        val groups = findMatchGroups()
        if (groups.isEmpty()) {
            pendingMatchGroups.clear()
            return false
        }

        pendingMatchGroups.clear()
        pendingMatchGroups.addAll(sortMatchGroups(groups))
        matchRemoveDelay = 0f
        return true
    }

    private fun startResolveMatches() {
        if (!preparePendingMatchGroups()) return

        resolvingMatches = true

        waitingBeforeGravity = false
        beforeGravityDelayRemaining = 0f

        gravityAnimating = false
        gravityAnimationRemaining = 0f

        waitingNextCascade = false
        nextCascadeDelayRemaining = 0f
    }

    override fun update(gctx: GameContext) {
        if (resolvingMatches) {
            if (gravityAnimating) {
                gravityAnimationRemaining -= gctx.frameTime
                if (gravityAnimationRemaining <= 0f) {
                    gravityAnimating = false
                    gravityAnimationRemaining = 0f
                    finishAfterGravityAnimation()
                }
                return
            }

            if (waitingBeforeGravity) {
                beforeGravityDelayRemaining -= gctx.frameTime
                if (beforeGravityDelayRemaining <= 0f) {
                    waitingBeforeGravity = false
                    beforeGravityDelayRemaining = 0f
                    applyGravityOrFinish()
                }
                return
            }

            if (waitingNextCascade) {
                nextCascadeDelayRemaining -= gctx.frameTime
                if (nextCascadeDelayRemaining <= 0f) {
                    waitingNextCascade = false
                    nextCascadeDelayRemaining = 0f
                    matchRemoveDelay = 0f
                }
                return
            }

            matchRemoveDelay -= gctx.frameTime

            if (matchRemoveDelay <= 0f) {
                if (pendingMatchGroups.isNotEmpty()) {
                    val nextGroup = pendingMatchGroups.removeAt(0)
                    removeMatchGroup(nextGroup)

                    if (pendingMatchGroups.isNotEmpty()) {
                        matchRemoveDelay = MATCH_GROUP_REMOVE_INTERVAL
                    } else {
                        startBeforeGravityDelay()
                    }
                }
            }
            return
        }

        if (!timerStarted || holdingDrop == null) return

        remainingHoldTime -= gctx.frameTime
        if (remainingHoldTime <= 0f) {
            remainingHoldTime = 0f

            val shouldResolve = swappedDuringHold
            clearHold()

            if (shouldResolve) {
                startResolveMatches()
            }

            swappedDuringHold = false
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
        if (resolvingMatches) return true
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                val cell = findVisibleCell(event.x, event.y) ?: return false
                beginHold(cell.first, cell.second)
                moveHoldingDrop(event.x, event.y)
                return true
            }

            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                val shouldResolve = swappedDuringHold

                clearHold()

                if (shouldResolve) {
                    startResolveMatches()
                }

                swappedDuringHold = false
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