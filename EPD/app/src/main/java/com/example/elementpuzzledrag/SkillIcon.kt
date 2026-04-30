package com.example.elementpuzzledrag

import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IBoxCollidable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

enum class SkillType {
    ATTACK_UP,
    DROP_CHANGE,
}

class SkillIcon(
    gctx: GameContext,
    val skillType: SkillType,
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