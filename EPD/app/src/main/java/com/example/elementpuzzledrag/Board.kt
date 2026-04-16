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
    }

    private val drops = Array(ROWS) { arrayOfNulls<Drop>(COLS) }

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

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
    }
}