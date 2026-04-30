package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class DropChangeTargetMarker(
    gctx: GameContext,
    slot: ElementSlot,
) : Sprite(gctx, R.mipmap.i_target_marker) {

    companion object {
        private const val MARKER_SIZE = 40f
        private const val MARKER_GAP = 4f
    }

    init {
        setSize(MARKER_SIZE, MARKER_SIZE)

        val centerX = slot.slotLeft + slot.slotWidth / 2f
        val centerY = slot.slotTop + slot.slotHeight + MARKER_GAP + MARKER_SIZE / 2f

        setCenter(centerX, centerY)
    }
}