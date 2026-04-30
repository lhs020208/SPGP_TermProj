package com.example.elementpuzzledrag

import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IBoxCollidable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class ElementSlot(
    gctx: GameContext,
    val elementType: DropType,
    val resId: Int,
    val slotLeft: Float,
    val slotTop: Float,
    val slotWidth: Float,
    val slotHeight: Float,
) : Sprite(gctx, resId), IBoxCollidable {

    override val collisionRect = RectF()

    init {
        setSize(slotWidth, slotHeight)
        setCenter(slotLeft + slotWidth / 2f, slotTop + slotHeight / 2f)
        updateCollisionRect()
    }

    private fun updateCollisionRect() {
        collisionRect.set(dstRect)
    }

    fun containsWorldPoint(px: Float, py: Float): Boolean {
        return collisionRect.contains(px, py)
    }
}