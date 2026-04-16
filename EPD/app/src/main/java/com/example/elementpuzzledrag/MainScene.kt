package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class MainScene(gctx: GameContext) : Scene(gctx) {
    override val world = World(arrayOf(
        //Layer.BACKGROUND,
        //Layer.BOARD,
        //Layer.HUD,
        Layer.BACKGROUND,
        Layer.HUD,
        Layer.BOARD,
    ))

    init {
        val screenW = gctx.metrics.width      // 900
        val screenH = gctx.metrics.height     // 1600
        val unitH = screenH / 16f             // 100

        val stageTop = 0f
        val stageHeight = 6.5f * unitH

        val elementTop = stageTop + stageHeight
        val elementHeight = 1.5f * unitH

        val hpTop = elementTop + elementHeight
        val hpHeight = 0.5f * unitH

        val puzzleTop = hpTop + hpHeight
        val puzzleHeight = 7.5f * unitH

        world.add(
            UiSprite(
                gctx,
                R.mipmap.stage1,
                0f,
                stageTop,
                screenW,
                stageHeight,
            ),
            Layer.HUD,
        )

        world.add(
            UiSprite(
                gctx,
                R.mipmap.i_element,
                0f,
                elementTop,
                screenW,
                elementHeight,
            ),
            Layer.HUD,
        )

        world.add(
            HpBar(
                gctx,
                0f,
                hpTop,
                screenW,
                hpHeight,
                1f,
            ),
            Layer.HUD,
        )

        world.add(
            UiSprite(
                gctx,
                R.mipmap.i_puzzle_plane,
                0f,
                puzzleTop,
                screenW,
                puzzleHeight,
            ),
            Layer.BACKGROUND,
        )
        val board = Board(gctx, world)
        //world.add(board, Layer.BOARD)
    }
}