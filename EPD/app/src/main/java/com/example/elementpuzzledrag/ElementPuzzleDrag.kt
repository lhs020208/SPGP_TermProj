package com.example.elementpuzzledrag

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kr.ac.tukorea.ge.spgp2026.a2dg.activity.BaseGameActivity
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class ElementPuzzleDrag : BaseGameActivity() {
    override val drawsDebugGrid: Boolean = false
    override val drawsDebugInfo: Boolean = false
    override val drawsFpsGraph: Boolean = false

    override fun createRootScene(gctx: GameContext): Scene {
        gctx.metrics.setSize(900f, 1600f)
        return MainScene(gctx)
    }
}