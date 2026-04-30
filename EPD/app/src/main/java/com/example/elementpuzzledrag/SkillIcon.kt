package com.example.elementpuzzledrag

import android.graphics.Canvas
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IBoxCollidable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

enum class SkillType {
    ATTACK_UP,
    DROP_CHANGE,
}

class SkillIcon(
    private val gctx: GameContext,
    val skillType: SkillType,
    resId: Int,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    val cooldownRemaining: Int,
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

    private fun waitOverlayResId(): Int? {
        return when (cooldownRemaining.coerceIn(0, 5)) {
            0 -> null
            1 -> R.mipmap.skill_wait1
            2 -> R.mipmap.skill_wait2
            3 -> R.mipmap.skill_wait3
            4 -> R.mipmap.skill_wait4
            else -> R.mipmap.skill_wait5
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val waitResId = waitOverlayResId() ?: return
        val waitBitmap = gctx.res.getBitmap(waitResId)
        canvas.drawBitmap(waitBitmap, null, dstRect, null)
    }
}