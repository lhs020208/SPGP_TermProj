package com.example.elementpuzzledrag

import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IBoxCollidable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class ElementSlot(
    gctx: GameContext,
    val elementType: DropType,
    resId: Int,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
) : Sprite(gctx, resId), IBoxCollidable {

    override val collisionRect = RectF()

    init {
        setSize(width, height)
        setCenter(left + width / 2f, top + height / 2f)
        updateCollisionRect()
    }

    private fun updateCollisionRect() {
        collisionRect.set(dstRect)
    }

    fun containsWorldPoint(px: Float, py: Float): Boolean {
        return collisionRect.contains(px, py)
    }
}