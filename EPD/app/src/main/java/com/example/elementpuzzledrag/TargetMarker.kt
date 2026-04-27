package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class TargetMarker(
    gctx: GameContext,
    private val target: Monster,
) : Sprite(gctx, R.mipmap.i_target_marker) {

    companion object {
        private const val MARKER_SIZE = 40f
        private const val MARKER_GAP = 8f
    }

    init {
        setSize(MARKER_SIZE, MARKER_SIZE)
        syncToTarget()
    }

    override fun update(gctx: GameContext) {
        syncToTarget()
    }

    private fun syncToTarget() {
        val (centerX, centerY) = target.getTargetMarkerCenter(
            markerHeight = MARKER_SIZE,
            markerGap = MARKER_GAP,
        )
        setCenter(centerX, centerY)
    }
}