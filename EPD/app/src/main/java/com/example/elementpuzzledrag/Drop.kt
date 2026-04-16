package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Drop(
    gctx: GameContext,
    var row: Int,
    var col: Int,
    var type: DropType,
) : Sprite(gctx, type.toResId()) {

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
        updatePosition()
    }

    fun setHolding(holding: Boolean) {
        if (holding) {
            width = HOLD_SIZE
            height = HOLD_SIZE
        } else {
            width = DROP_SIZE
            height = DROP_SIZE
        }
    }
}