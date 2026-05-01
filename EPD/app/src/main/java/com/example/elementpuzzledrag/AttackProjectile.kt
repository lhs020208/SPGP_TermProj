package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.sqrt

class AttackProjectile(
    gctx: GameContext,
    private val world: World<Layer>,
    resId: Int,
    startX: Float,
    startY: Float,
    private val targetX: Float,
    private val targetY: Float,
    size: Float,
    private val speed: Float,
    private val onArrived: () -> Unit,
) : Sprite(gctx, resId) {

    private var arrived = false

    init {
        setSize(size, size)
        setCenter(startX, startY)
    }

    override fun update(gctx: GameContext) {
        if (arrived) return

        val dx = targetX - x
        val dy = targetY - y
        val distance = sqrt(dx * dx + dy * dy)

        val moveDistance = speed * gctx.frameTime

        if (distance <= moveDistance || distance <= 1f) {
            arrived = true
            setCenter(targetX, targetY)
            world.remove(this, Layer.ATTACK)
            onArrived()
            return
        }

        val nx = dx / distance
        val ny = dy / distance

        setCenter(
            x + nx * moveDistance,
            y + ny * moveDistance,
        )
    }
}