package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import android.view.MotionEvent

class MainScene(gctx: GameContext) : Scene(gctx) {

    companion object {
        private const val SHOW_HIDDEN_DROPS_FOR_DEBUG = false
    }
    override val world = World(
        if (SHOW_HIDDEN_DROPS_FOR_DEBUG) {
            arrayOf(
                Layer.PUZZLE_BG,
                Layer.STAGE,
                Layer.HUD,
                Layer.BOARD,
                Layer.HOLDING,
                Layer.OVERLAY,
                Layer.MONSTER,
            )
        } else {
            arrayOf(
                Layer.PUZZLE_BG,
                Layer.BOARD,
                Layer.STAGE,
                Layer.HUD,
                Layer.HOLDING,
                Layer.OVERLAY,
                Layer.MONSTER,
            )
        }
    )

    private lateinit var board: Board

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

        val typeGuideMargin = 20f
        val typeGuideWidth = 130f
        val typeGuideHeight = typeGuideWidth * 440f / 327f

        world.add(
            UiSprite(
                gctx,
                R.mipmap.stage1,
                0f,
                stageTop,
                screenW,
                stageHeight,
            ),
            Layer.STAGE,
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
            UiSprite(
                gctx,
                R.mipmap.type,
                typeGuideMargin,
                typeGuideMargin,
                typeGuideWidth,
                typeGuideHeight,
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
            Layer.PUZZLE_BG,
        )

        world.add(
            Monster(
                gameContext = gctx,
                attribute = DropType.FIRE,
                attackPower = 100,
                hp = 500,
                MaxremainingAttackTurns = 3,
                hpGaugeSize = Monster.HpGaugeSize.SMALL,
                resId = R.mipmap.m_firemonmus,
                centerX = screenW / 2f,
                centerY = stageTop + stageHeight / 2f,
            ),
            Layer.MONSTER,
        )

        world.add(
            Monster(
                gameContext = gctx,
                attribute = DropType.LEAF,
                attackPower = 100,
                hp = 500,
                MaxremainingAttackTurns = 3,
                hpGaugeSize = Monster.HpGaugeSize.SMALL,
                resId = R.mipmap.m_leafmonmus,
                centerX = screenW / 2f - 300,
                centerY = stageTop + stageHeight / 2f + 100,
            ),
            Layer.MONSTER,
        )

        world.add(
            Monster(
                gameContext = gctx,
                attribute = DropType.WATER,
                attackPower = 100,
                hp = 500,
                MaxremainingAttackTurns = 3,
                hpGaugeSize = Monster.HpGaugeSize.SMALL,
                resId = R.mipmap.m_watermonmus,
                centerX = screenW / 2f + 300,
                centerY = stageTop + stageHeight / 2f + 100,
            ),
            Layer.MONSTER,
        )
        board = Board(gctx, world)
        world.add(board, Layer.OVERLAY)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return board.onTouchEvent(event)
    }
}