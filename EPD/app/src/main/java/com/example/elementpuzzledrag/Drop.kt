package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Drop(
    gctx: GameContext,
    var row: Int,
    var col: Int,
    var type: DropType,
    private val cellSize: Float,
    private val boardLeft: Float,
    private val boardTop: Float,
) : Sprite(gctx, type.toResId()) {

    init {
        width = cellSize
        height = cellSize
        updatePosition()
    }

    fun updatePosition() {
        x = boardLeft + col * cellSize + cellSize / 2f
        y = boardTop + row * cellSize + cellSize / 2f
    }

    fun setGridPosition(row: Int, col: Int) {
        this.row = row
        this.col = col
        updatePosition()
    }
}