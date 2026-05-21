package com.example.elementpuzzledrag

import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IRecyclable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.sqrt

class AttackProjectile private constructor(
    private val gctx: GameContext,
) : Sprite(gctx, R.mipmap.a_fire), IRecyclable {

    companion object {
        fun get(
            gctx: GameContext,
            world: World<Layer>,
            resId: Int,
            startX: Float,
            startY: Float,
            targetX: Float,
            targetY: Float,
            size: Float,
            speed: Float,
            onArrived: () -> Unit,
        ): AttackProjectile {
            val projectile = world.obtain(AttackProjectile::class.java)
                ?: AttackProjectile(gctx)

            return projectile.init(
                world = world,
                resId = resId,
                startX = startX,
                startY = startY,
                targetX = targetX,
                targetY = targetY,
                size = size,
                speed = speed,
                onArrived = onArrived,
            )
        }
    }

    private lateinit var world: World<Layer>

    private var targetX = 0f
    private var targetY = 0f
    private var speed = 0f
    private var arrived = false
    private var onArrived: (() -> Unit)? = null

    private fun init(
        world: World<Layer>,
        resId: Int,
        startX: Float,
        startY: Float,
        targetX: Float,
        targetY: Float,
        size: Float,
        speed: Float,
        onArrived: () -> Unit,
    ): AttackProjectile {
        this.world = world

        bitmap = gctx.res.getBitmap(resId)

        this.targetX = targetX
        this.targetY = targetY
        this.speed = speed
        this.onArrived = onArrived

        arrived = false

        setSize(size, size)
        setCenter(startX, startY)

        return this
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

            val callback = onArrived

            world.remove(this, Layer.ATTACK)
            callback?.invoke()

            return
        }

        val nx = dx / distance
        val ny = dy / distance

        setCenter(
            x + nx * moveDistance,
            y + ny * moveDistance,
        )
    }

    override fun onRecycle() {
        arrived = false
        targetX = 0f
        targetY = 0f
        speed = 0f
        onArrived = null
    }
}