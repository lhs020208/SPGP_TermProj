package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

open class UiSprite(
    gctx: GameContext,
    resId: Int,
    left: Float,
    top: Float,
    drawWidth: Float,
    drawHeight: Float,
) : Sprite(gctx, resId) {

    init {
        width = drawWidth
        height = drawHeight
        x = left + drawWidth / 2f
        y = top + drawHeight / 2f
    }
}