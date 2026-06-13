package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.activity.BaseGameActivity
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class ElementPuzzleDrag : BaseGameActivity() {

    companion object {
        const val EXTRA_INITIAL_STAGE_INDEX = "initial_stage_index"
    }

    override val drawsDebugGrid: Boolean = false
    override val drawsDebugInfo: Boolean = false
    override val drawsFpsGraph: Boolean = false

    override fun createRootScene(gctx: GameContext): Scene {
        gctx.metrics.setSize(900f, 1600f)

        val initialStageIndex = intent
            .getIntExtra(EXTRA_INITIAL_STAGE_INDEX, 0)
            .coerceIn(0, StageData.stages.lastIndex)

        return MainScene(
            gctx = gctx,
            initialStageIndex = initialStageIndex,
        )
    }
}